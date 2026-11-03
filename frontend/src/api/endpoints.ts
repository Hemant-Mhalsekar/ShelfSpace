import { api, download } from './client'
import type {
  AdminDashboard, AuditCategory, AuditEntry, AuthResponse, Book, BookInput, Copy, CopyStatus, Fine, FineStatus, FineSummary, Loan, LoanStatus,
  MemberDashboard, MemberDetail, MemberSummary, MembershipTier, Notification, Page, Policy, PolicyInput,
  Recommendation, Reservation, ReturnResult, Role, User, WishlistItem,
} from './types'

function query(params: Record<string, string | number | boolean | undefined | null>) {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') q.set(k, String(v))
  }
  const s = q.toString()
  return s ? `?${s}` : ''
}

export const authApi = {
  login: (email: string, password: string) =>
    api<AuthResponse>('/auth/login', { method: 'POST', body: { email, password } }),
  register: (body: { name: string; email: string; phone?: string; password: string }) =>
    api<AuthResponse>('/auth/register', { method: 'POST', body }),
  me: () => api<User>('/auth/me'),
  logout: (refreshToken: string) => api<void>('/auth/logout', { method: 'POST', body: { refreshToken } }),
}

export const accountApi = {
  updateProfile: (body: { name: string; phone: string | null }) =>
    api<User>('/account/profile', { method: 'PUT', body }),
  /** Signs out other devices; returns fresh tokens for this one. */
  changePassword: (currentPassword: string, newPassword: string) =>
    api<AuthResponse>('/account/password', { method: 'POST', body: { currentPassword, newPassword } }),
  signOutEverywhere: () => api<AuthResponse>('/account/sign-out-everywhere', { method: 'POST' }),
}

interface BookSearch {
  q?: string
  category?: string
  availableOnly?: boolean
  page?: number
  size?: number
  sort?: string
}

export const booksApi = {
  search: (s: BookSearch) => api<Page<Book>>(`/books${query({ ...s })}`),
  get: (id: number) => api<Book>(`/books/${id}`),
  categories: () => api<string[]>('/books/categories'),
}

export const loansApi = {
  request: (bookId: number) => api<Loan>('/loans', { method: 'POST', body: { bookId } }),
  mine: (status?: string, page = 0) => api<Page<Loan>>(`/loans/me${query({ status, page, size: 20 })}`),
  renew: (id: number) => api<Loan>(`/loans/${id}/renew`, { method: 'POST' }),
  cancel: (id: number) => api<Loan>(`/loans/${id}/cancel`, { method: 'POST' }),
}

export const reservationsApi = {
  reserve: (bookId: number) => api<Reservation>('/reservations', { method: 'POST', body: { bookId } }),
  mine: () => api<Reservation[]>('/reservations/me'),
  cancel: (id: number) => api<Reservation>(`/reservations/${id}/cancel`, { method: 'POST' }),
}

export const finesApi = {
  mine: () => api<FineSummary>('/fines/me'),
}

export const notificationsApi = {
  list: (unreadOnly = false) => api<Page<Notification>>(`/notifications/me${query({ unreadOnly, size: 50 })}`),
  unreadCount: () => api<{ unread: number }>('/notifications/me/unread-count'),
  markRead: (id: number) => api<Notification>(`/notifications/${id}/read`, { method: 'POST' }),
  markAllRead: () => api<{ updated: number }>('/notifications/me/read-all', { method: 'POST' }),
}

export const dashboardApi = {
  me: () => api<MemberDashboard>('/dashboard/me'),
  admin: () => api<AdminDashboard>('/dashboard/admin'),
}

export const policiesApi = {
  list: () => api<Policy[]>('/policies'),
  get: (tier: MembershipTier) => api<Policy>(`/policies/${tier}`),
  update: (tier: MembershipTier, body: PolicyInput) => api<Policy>(`/policies/${tier}`, { method: 'PUT', body }),
}

// ---------- Librarian-only endpoints ----------

export const adminBooksApi = {
  create: (body: BookInput) => api<Book>('/books', { method: 'POST', body }),
  update: (id: number, body: BookInput) => api<Book>(`/books/${id}`, { method: 'PUT', body }),
  remove: (id: number) => api<void>(`/books/${id}`, { method: 'DELETE' }),
  copies: (id: number) => api<Copy[]>(`/books/${id}/copies`),
  addCopies: (id: number, count: number) => api<Copy[]>(`/books/${id}/copies`, { method: 'POST', body: { count } }),
  setCopyStatus: (copyId: number, status: CopyStatus, note?: string) =>
    api<Copy>(`/copies/${copyId}/status`, { method: 'PATCH', body: { status, note } }),
}

export const adminLoansApi = {
  search: (p: { status?: LoanStatus; memberId?: number; page?: number; sort?: string }) =>
    api<Page<Loan>>(`/loans${query({ ...p, size: 20 })}`),
  approve: (id: number) => api<Loan>(`/loans/${id}/approve`, { method: 'POST' }),
  reject: (id: number, reason?: string) => api<Loan>(`/loans/${id}/reject`, { method: 'POST', body: { reason } }),
  returnBook: (id: number) => api<ReturnResult>(`/loans/${id}/return`, { method: 'POST' }),
  markLost: (id: number) => api<ReturnResult>(`/loans/${id}/lost`, { method: 'POST' }),
}

export interface MemberSearch {
  q?: string
  status?: 'ACTIVE' | 'SUSPENDED'
  tier?: MembershipTier
  role?: Role
  page?: number
}

export const membersApi = {
  search: (s: MemberSearch) => api<Page<MemberSummary>>(`/members${query({ ...s, size: 20 })}`),
  get: (id: number) => api<MemberDetail>(`/members/${id}`),
  create: (body: { name: string; email: string; phone?: string; password: string; membershipTier: MembershipTier; role?: Role }) =>
    api<MemberSummary>('/members', { method: 'POST', body }),
  update: (id: number, body: { name: string; phone?: string | null; membershipTier: MembershipTier }) =>
    api<MemberSummary>(`/members/${id}`, { method: 'PUT', body }),
  suspend: (id: number, reason: string) => api<MemberSummary>(`/members/${id}/suspend`, { method: 'POST', body: { reason } }),
  activate: (id: number) => api<MemberSummary>(`/members/${id}/activate`, { method: 'POST' }),
  resetPassword: (id: number, newPassword: string) =>
    api<void>(`/members/${id}/reset-password`, { method: 'POST', body: { newPassword } }),
}

export const adminFinesApi = {
  search: (p: { status?: FineStatus; memberId?: number; page?: number }) =>
    api<Page<Fine>>(`/fines${query({ ...p, size: 20 })}`),
  pay: (id: number) => api<Fine>(`/fines/${id}/pay`, { method: 'POST' }),
  waive: (id: number, note: string) => api<Fine>(`/fines/${id}/waive`, { method: 'POST', body: { note } }),
}

export const wishlistApi = {
  list: () => api<WishlistItem[]>('/wishlist'),
  add: (bookId: number) => api<WishlistItem>('/wishlist', { method: 'POST', body: { bookId } }),
  remove: (bookId: number) => api<void>(`/wishlist/${bookId}`, { method: 'DELETE' }),
}

export const recommendationsApi = {
  forBook: (bookId: number, limit = 5) => api<Recommendation[]>(`/books/${bookId}/recommendations?limit=${limit}`),
  mine: (limit = 6) => api<Recommendation[]>(`/recommendations/me?limit=${limit}`),
}

export interface AuditSearch {
  category?: AuditCategory
  action?: string
  actorId?: number
  entityType?: string
  entityId?: number
  from?: string
  to?: string
  page?: number
}

export const auditApi = {
  search: (s: AuditSearch) => api<Page<AuditEntry>>(`/audit${query({ ...s, size: 30 })}`),
  actions: () => api<string[]>('/audit/actions'),
  exportCsv: (s: AuditSearch) => download(`/audit/export${query({ ...s, page: undefined })}`),
}
