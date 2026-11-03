import { COMMON_PASSWORDS } from './common-passwords'

/**
 * Field rules shown while typing. They mirror the server's rules (common/validation in the API),
 * so a form that passes here won't be refused there. Each returns a message, or null if the value is fine.
 */

export interface Person { name?: string | null; email?: string | null }

const PASSWORD_MIN = 8
const PASSWORD_MAX = 72

function isCommonPassword(password: string) {
  const lower = password.toLowerCase()
  const base = lower.replace(/^[^\p{L}]+|[^\p{L}]+$/gu, '')
  return COMMON_PASSWORDS.has(base) || COMMON_PASSWORDS.has(lower)
}

function containsPersonal(password: string, { name, email }: Person = {}) {
  const pw = password.toLowerCase()
  const emailName = (email ?? '').trim().toLowerCase().split('@')[0] ?? ''
  if (emailName.length >= 3 && pw.includes(emailName)) return true
  return (name ?? '').toLowerCase().split(/[^\p{L}]+/u).some((part) => part.length >= 3 && pw.includes(part))
}

/** The checklist under a new-password field. */
export function passwordChecks(password: string, person?: Person) {
  return [
    { label: `${PASSWORD_MIN} or more characters`, ok: password.length >= PASSWORD_MIN && password.length <= PASSWORD_MAX },
    { label: 'Upper and lowercase letters', ok: /\p{Lu}/u.test(password) && /\p{Ll}/u.test(password) },
    { label: 'At least one number', ok: /\d/.test(password) },
    { label: 'Not a common password', ok: password.length > 0 && !isCommonPassword(password) },
    ...(person && (person.name || person.email)
      ? [{ label: "Doesn't contain your name or email", ok: password.length > 0 && !containsPersonal(password, person) }]
      : []),
  ]
}

export function passwordProblem(password: string, person?: Person): string | null {
  if (!password) return 'Enter a password'
  if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) return `Must be ${PASSWORD_MIN}-${PASSWORD_MAX} characters`
  if (password !== password.trim()) return 'Must not start or end with a space'
  if (!/\p{Lu}/u.test(password) || !/\p{Ll}/u.test(password)) return 'Needs both an uppercase and a lowercase letter'
  if (!/\d/.test(password)) return 'Needs at least one number'
  if (isCommonPassword(password)) return 'Too common. Choose something harder to guess'
  if (person && containsPersonal(password, person)) return 'Must not contain your name or email'
  return null
}

/** Weak until every rule passes; Strong once it's also 12+ characters. */
export function passwordStrength(password: string, person?: Person): 'weak' | 'good' | 'strong' {
  if (passwordProblem(password, person)) return 'weak'
  return password.length >= 12 ? 'strong' : 'good'
}

export function nameProblem(value: string): string | null {
  const name = value.trim()
  if (!name) return 'Enter a name'
  if (name.length < 2 || name.length > 100) return 'Must be 2-100 characters'
  if (!/^\p{L}[\p{L}\p{M} .'-]*$/u.test(name)) return 'Use letters, spaces, apostrophes, hyphens and dots only'
  return null
}

export function emailProblem(value: string): string | null {
  const email = value.trim()
  if (!email) return 'Enter an email address'
  if (email.length > 150 || email.includes('..') || !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}$/.test(email)) {
    return 'Enter a valid email address, like name@example.com'
  }
  return null
}

/** Optional field: empty is fine. */
export function phoneProblem(value: string): string | null {
  const phone = value.trim()
  if (!phone) return null
  return /^\+?[0-9]{10,15}$/.test(phone) ? null : 'Must be 10-15 digits, optionally starting with +'
}

export function requiredText(value: string, what: string, { needsLetter = false, max = 255 } = {}): string | null {
  const text = value.trim()
  if (!text) return `Enter ${what}`
  if (text.length > max) return `Must be at most ${max} characters`
  if (needsLetter && !/\p{L}/u.test(text)) return 'Must contain a letter'
  return null
}

export function isbnProblem(value: string): string | null {
  const isbn = value.replace(/[\s-]/g, '').toUpperCase()
  if (!isbn) return 'Enter the ISBN'
  const ok = isbn.length === 13 ? /^\d{13}$/.test(isbn) && [...isbn].reduce((s, d, i) => s + Number(d) * (i % 2 ? 3 : 1), 0) % 10 === 0
    : isbn.length === 10 ? /^\d{9}[\dX]$/.test(isbn) && [...isbn].reduce((s, d, i) => s + (d === 'X' ? 10 : Number(d)) * (10 - i), 0) % 11 === 0
      : false
  return ok ? null : 'Not a valid ISBN-10 or ISBN-13. Check the digits'
}

/** Optional. */
export function yearProblem(value: string, now = new Date()): string | null {
  if (!value.trim()) return null
  const year = Number(value)
  if (!Number.isInteger(year)) return 'Enter a whole year, like 2018'
  if (year < 1450) return 'Must be 1450 or later'
  if (year > now.getFullYear() + 1) return "Can't be later than next year"
  return null
}

/** Optional. */
export function costProblem(value: string): string | null {
  if (!value.trim()) return null
  const cost = Number(value)
  if (Number.isNaN(cost) || cost < 0) return 'Must be 0 or more'
  if (cost > 1_000_000) return 'Must be at most 1000000'
  if (!/^\d+(\.\d{1,2})?$/.test(value.trim())) return 'Use at most 2 decimal places'
  return null
}

export function wholeNumberProblem(value: string, min: number, max: number): string | null {
  const n = Number(value)
  if (value.trim() === '' || !Number.isInteger(n)) return 'Enter a whole number'
  if (n < min || n > max) return `Must be ${min}-${max}`
  return null
}

/** Server messages follow the field name ("needs a number"); start them with a capital like ours. */
export function serverMessage(message: string | undefined) {
  return message ? message.charAt(0).toUpperCase() + message.slice(1) : undefined
}
