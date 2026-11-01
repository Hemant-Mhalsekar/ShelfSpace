import { api } from './client'
import type {
  AuthResponse, Book, FineSummary, Loan, MemberDashboard, Notification, Page, Reservation, User,
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
}

export interface BookSearch {
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
}
