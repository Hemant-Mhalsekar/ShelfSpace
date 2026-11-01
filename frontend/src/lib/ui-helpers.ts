import { ApiError } from '../api/client'

/** Readable message for any error thrown by a query or mutation. */
export function errorMessage(error: unknown) {
  if (error instanceof ApiError) return error.message
  if (error instanceof Error) return error.message
  return 'Something went wrong'
}

export const inputClass =
  'block w-full rounded-xl border-0 bg-canvas-100 px-3.5 py-2.5 text-[15px] text-ink-900 ' +
  'placeholder:text-ink-500 transition-colors hover:bg-canvas-200 focus:bg-canvas-50 focus:ring-2 focus:ring-accent-400 focus:outline-none'
