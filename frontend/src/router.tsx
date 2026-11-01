import { createBrowserRouter } from 'react-router'
import { HomeRedirect } from './components/HomeRedirect'
import { Layout } from './components/Layout'
import { RequireAdmin } from './components/RequireAdmin'
import { RequireAuth } from './components/RequireAuth'
import { AdminBookPage } from './pages/admin/AdminBookPage'
import { AdminBooksPage } from './pages/admin/AdminBooksPage'
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage'
import { AdminFinesPage } from './pages/admin/AdminFinesPage'
import { LoanDeskPage } from './pages/admin/LoanDeskPage'
import { MemberPage } from './pages/admin/MemberPage'
import { MembersPage } from './pages/admin/MembersPage'
import { RulesPage } from './pages/admin/RulesPage'
import { BookPage } from './pages/BookPage'
import { CatalogPage } from './pages/CatalogPage'
import { DashboardPage } from './pages/DashboardPage'
import { FinesPage } from './pages/FinesPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { RegisterPage } from './pages/RegisterPage'
import { WishlistPage } from './pages/WishlistPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <Layout />,
        children: [
          { index: true, element: <HomeRedirect /> },
          { path: '/books', element: <CatalogPage /> },
          { path: '/books/:id', element: <BookPage /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/fines', element: <FinesPage /> },
          { path: '/wishlist', element: <WishlistPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          {
            path: '/admin',
            element: <RequireAdmin />,
            children: [
              { index: true, element: <AdminDashboardPage /> },
              { path: 'loans', element: <LoanDeskPage /> },
              { path: 'books', element: <AdminBooksPage /> },
              { path: 'books/:id', element: <AdminBookPage /> },
              { path: 'members', element: <MembersPage /> },
              { path: 'members/:id', element: <MemberPage /> },
              { path: 'fines', element: <AdminFinesPage /> },
              { path: 'rules', element: <RulesPage /> },
            ],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
