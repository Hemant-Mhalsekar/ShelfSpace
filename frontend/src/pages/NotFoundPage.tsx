import { Link } from 'react-router'

export function NotFoundPage() {
  return (
    <div className="py-20 text-center">
      <p className="text-sm font-semibold text-accent-600">404</p>
      <h1 className="mt-2 text-2xl font-bold">Page not found</h1>
      <Link to="/books" className="mt-6 inline-block text-sm font-medium text-accent-600 hover:underline">Back to the catalog</Link>
    </div>
  )
}
