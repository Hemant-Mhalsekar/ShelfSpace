// Shapes of the JSON returned by the ShelfSpace API (see Swagger UI for the source of truth).

export type Role = 'ADMIN' | 'MEMBER'
export type MembershipTier = 'STUDENT' | 'FACULTY' | 'GUEST'

export interface User {
  id: number
  name: string
  email: string
  phone: string | null
  role: Role
  membershipTier: MembershipTier
  status: 'ACTIVE' | 'SUSPENDED'
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface Book {
  id: number
  title: string
  author: string
  isbn: string
  category: string
  publicationYear: number | null
  replacementCost: number | null
  totalCopies: number
  availableCopies: number
}

export type LoanStatus =
  | 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  | 'ISSUED' | 'OVERDUE' | 'RETURNED' | 'LOST'

export interface Loan {
  id: number
  status: LoanStatus
  bookId: number | null
  bookTitle: string
  copyBarcode: string
  memberId: number
  memberName: string
  requestedAt: string
  borrowedAt: string | null
  dueDate: string | null
  returnedAt: string | null
  renewCount: number
  overdueDays: number | null
  accruedFine: number | null
}

export type ReservationStatus = 'WAITING' | 'READY' | 'FULFILLED' | 'EXPIRED' | 'CANCELLED'

export interface Reservation {
  id: number
  bookId: number
  bookTitle: string
  status: ReservationStatus
  position: number | null
  estimatedAvailableDate: string | null
  heldCopyBarcode: string | null
  expiresAt: string | null
  createdAt: string
}

export type FineStatus = 'UNPAID' | 'PAID' | 'WAIVED'

export interface Fine {
  id: number
  loanId: number
  memberId: number
  bookTitle: string
  amount: number
  reason: 'OVERDUE' | 'LOST' | 'DAMAGED'
  status: FineStatus
  createdAt: string
  paidAt: string | null
  note: string | null
}

export interface FineSummary {
  totalUnpaid: number
  fines: Fine[]
}

export interface Notification {
  id: number
  type: string
  title: string
  message: string
  referenceType: string | null
  referenceId: number | null
  read: boolean
  createdAt: string
}

export interface MemberDashboard {
  summary: {
    booksOut: number
    booksAllowed: number
    dueWithin3Days: number
    overdue: number
    unpaidFines: number
    unreadNotifications: number
  }
  currentLoans: Loan[]
  reservations: Reservation[]
  recentlyReturned: Loan[]
}

export interface ApiErrorBody {
  status: number
  error: string
  message: string
  path: string
  fieldErrors: Record<string, string> | null
}
