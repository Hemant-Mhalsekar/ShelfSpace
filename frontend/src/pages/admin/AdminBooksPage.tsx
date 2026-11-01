import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { adminBooksApi, booksApi } from '../../api/endpoints'
import { Modal } from '../../components/Modal'
import { useToast } from '../../components/toast/useToast'
import { Alert, Button, Card, EmptyState, PageHeader, PageLoader, Pagination } from '../../components/ui'
import { errorMessage, inputClass } from '../../lib/ui-helpers'
import { Availability } from '../CatalogPage'
import { BookForm } from './BookForm'

export function AdminBooksPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const toast = useToast()
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const [adding, setAdding] = useState(false)

  const books = useQuery({
    queryKey: ['books', 'admin', q, page],
    queryFn: () => booksApi.search({ q, page, size: 20, sort: 'createdAt,desc' }),
    placeholderData: keepPreviousData,
  })

  return (
    <>
      <PageHeader title="Books" subtitle="Add titles, manage copies and fix details" actions={<Button onClick={() => setAdding(true)}>+ Add book</Button>} />

      <input
        type="search"
        className={`${inputClass} mb-5 max-w-md`}
        placeholder="Search title, author or ISBN…"
        value={q}
        onChange={(e) => { setQ(e.target.value); setPage(0) }}
      />

      {books.isPending ? <PageLoader />
        : books.isError ? <Alert>{errorMessage(books.error)}</Alert>
          : books.data.content.length === 0 ? <EmptyState title="No books yet">Add your first book to get started.</EmptyState>
            : (
              <>
                <Card className="overflow-x-auto">
                  <table className="w-full text-left text-sm">
                    <thead className="border-b border-slate-200 text-xs tracking-wide text-slate-500 uppercase">
                      <tr>
                        <th className="px-4 py-3 font-medium">Title</th>
                        <th className="px-4 py-3 font-medium">ISBN</th>
                        <th className="px-4 py-3 font-medium">Category</th>
                        <th className="px-4 py-3 font-medium">Copies</th>
                        <th className="px-4 py-3"><span className="sr-only">Manage</span></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {books.data.content.map((b) => (
                        <tr key={b.id} className="hover:bg-slate-50">
                          <td className="px-4 py-3">
                            <p className="font-medium">{b.title}</p>
                            <p className="text-xs text-slate-500">{b.author}</p>
                          </td>
                          <td className="px-4 py-3 font-mono text-xs text-slate-600">{b.isbn}</td>
                          <td className="px-4 py-3 text-slate-600">{b.category}</td>
                          <td className="px-4 py-3"><Availability book={b} /></td>
                          <td className="px-4 py-3 text-right">
                            <Link to={`/admin/books/${b.id}`} className="font-medium text-indigo-600 hover:underline">Manage</Link>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </Card>
                <Pagination page={books.data.page} totalPages={books.data.totalPages} onChange={setPage} />
              </>
            )}

      <Modal title="Add a book" open={adding} onClose={() => setAdding(false)} wide>
        <BookForm
          withCopies
          submitLabel="Add book"
          onCancel={() => setAdding(false)}
          onSubmit={async (input) => {
            const book = await adminBooksApi.create(input)
            void queryClient.invalidateQueries({ queryKey: ['books'] })
            void queryClient.invalidateQueries({ queryKey: ['categories'] })
            toast.show(`Added “${book.title}” with ${book.totalCopies} cop${book.totalCopies === 1 ? 'y' : 'ies'}`)
            setAdding(false)
            navigate(`/admin/books/${book.id}`)
          }}
        />
      </Modal>
    </>
  )
}
