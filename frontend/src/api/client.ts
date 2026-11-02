import type { ApiErrorBody, AuthResponse } from './types'

const ACCESS_KEY = 'shelfspace.accessToken'
const REFRESH_KEY = 'shelfspace.refreshToken'
const BASE = '/api/v1'

/** Error thrown for any non-2xx response, carrying the API's message and field errors. */
export class ApiError extends Error {
  status: number
  fieldErrors: Record<string, string>

  constructor(status: number, message: string, fieldErrors?: Record<string, string> | null) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors ?? {}
  }
}

// ---------- token storage ----------
// Kept in localStorage for simplicity. A production app might use httpOnly cookies instead.

export const tokens = {
  get access() {
    return safeGet(ACCESS_KEY)
  },
  get refresh() {
    return safeGet(REFRESH_KEY)
  },
  save(auth: AuthResponse) {
    safeSet(ACCESS_KEY, auth.accessToken)
    safeSet(REFRESH_KEY, auth.refreshToken)
  },
  clear() {
    safeRemove(ACCESS_KEY)
    safeRemove(REFRESH_KEY)
  },
}

/** Fired when the session can't be refreshed, so the app can send the user to the login page. */
export const SESSION_EXPIRED_EVENT = 'shelfspace:session-expired'

// ---------- request helper ----------

type Method = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export async function api<T>(path: string, options: { method?: Method; body?: unknown } = {}): Promise<T> {
  const response = await send(path, options, tokens.access)

  // Access tokens last 15 minutes. On a 401, try one silent refresh, then retry the request once.
  if (response.status === 401 && tokens.refresh && !path.startsWith('/auth/')) {
    const refreshed = await refreshTokens()
    if (refreshed) {
      return parse<T>(await send(path, options, tokens.access))
    }
    tokens.clear()
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  }
  return parse<T>(response)
}

/**
 * Downloads a file from an authenticated endpoint (a plain link can't send the token).
 * The server's Content-Disposition decides the file name.
 */
export async function download(path: string): Promise<void> {
  // Not send(): that asks for JSON, and the server would refuse to answer a CSV endpoint with JSON.
  const get = () => fetch(BASE + path, { headers: tokens.access ? { Authorization: `Bearer ${tokens.access}` } : {} })
  let response = await get()
  if (response.status === 401 && tokens.refresh && (await refreshTokens())) {
    response = await get()
  }
  if (!response.ok) {
    await parse(response)   // throws ApiError with the server's message
  }
  const name = /filename="([^"]+)"/.exec(response.headers.get('Content-Disposition') ?? '')?.[1] ?? 'download'
  const url = URL.createObjectURL(await response.blob())
  const a = document.createElement('a')
  a.href = url
  a.download = name
  document.body.append(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function send(path: string, { method = 'GET', body }: { method?: Method; body?: unknown }, token: string | null) {
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers.Authorization = `Bearer ${token}`
  return fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

async function parse<T>(response: Response): Promise<T> {
  if (response.status === 204) return undefined as T
  const text = await response.text()
  const data: unknown = text ? JSON.parse(text) : undefined
  if (!response.ok) {
    const err = (data ?? {}) as Partial<ApiErrorBody>
    throw new ApiError(response.status, err.message ?? `Request failed (${response.status})`, err.fieldErrors)
  }
  return data as T
}

// Several requests can 401 at once; they should share a single refresh call.
let refreshInFlight: Promise<boolean> | null = null

function refreshTokens(): Promise<boolean> {
  refreshInFlight ??= (async () => {
    const used = tokens.refresh
    try {
      const res = await send('/auth/refresh', { method: 'POST', body: { refreshToken: used } }, null)
      // Refresh tokens work once. If another tab refreshed first, it already saved a newer pair.
      if (!res.ok) return tokens.refresh !== null && tokens.refresh !== used
      tokens.save((await res.json()) as AuthResponse)
      return true
    } catch {
      return false
    } finally {
      refreshInFlight = null
    }
  })()
  return refreshInFlight
}

function safeGet(key: string) {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key: string, value: string) {
  try {
    localStorage.setItem(key, value)
  } catch {
    /* storage unavailable (e.g. private mode); the session lasts until reload */
  }
}

function safeRemove(key: string) {
  try {
    localStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}
