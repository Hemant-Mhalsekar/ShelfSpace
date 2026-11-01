import { createBrowserRouter, Navigate } from 'react-router'
import { Layout } from './components/Layout'
import { RequireAuth } from './components/RequireAuth'
import { BookPage } from './pages/BookPage'
import { CatalogPage } from './pages/CatalogPage'
import { DashboardPage } from './pages/DashboardPage'
import { FinesPage } from './pages/FinesPage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { NotificationsPage } from './pages/NotificationsPage'
import { RegisterPage } from './pages/RegisterPage'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <Layout />,
        children: [
          { index: true, element: <Navigate to="/books" replace /> },
          { path: '/books', element: <CatalogPage /> },
          { path: '/books/:id', element: <BookPage /> },
          { path: '/dashboard', element: <DashboardPage /> },
          { path: '/fines', element: <FinesPage /> },
          { path: '/notifications', element: <NotificationsPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
