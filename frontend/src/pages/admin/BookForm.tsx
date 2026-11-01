import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import type { Book, BookInput } from '../../api/types'
import { Alert, Button, Field } from '../../components/ui'
import { errorMessage, inputClass } from '../../lib/ui-helpers'

/** Used for both "Add book" (with initial copies) and "Edit book". */
export function BookForm({ initial, withCopies, submitLabel, onSubmit, onCancel }: {
  initial?: Book
  withCopies?: boolean
  submitLabel: string
  onSubmit: (input: BookInput) => Promise<unknown>
  onCancel?: () => void
}) {
  const [form, setForm] = useState({
    title: initial?.title ?? '',
    author: initial?.author ?? '',
    isbn: initial?.isbn ?? '',
    category: initial?.category ?? '',
    publicationYear: initial?.publicationYear?.toString() ?? '',
    replacementCost: initial?.replacementCost?.toString() ?? '',
    initialCopies: '1',
  })
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [busy, setBusy] = useState(false)

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError(null)
    setFieldErrors({})
    try {
      await onSubmit({
        title: form.title,
        author: form.author,
        isbn: form.isbn,
        category: form.category,
        publicationYear: form.publicationYear ? Number(form.publicationYear) : null,
        replacementCost: form.replacementCost ? Number(form.replacementCost) : null,
        ...(withCopies ? { initialCopies: Number(form.initialCopies || 0) } : {}),
      })
    } catch (err) {
      if (err instanceof ApiError) setFieldErrors(err.fieldErrors)
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {error && <Alert>{error}</Alert>}
      <Field label="Title" error={fieldErrors.title}>
        <input className={inputClass} required value={form.title} onChange={set('title')} />
      </Field>
      <Field label="Author" error={fieldErrors.author}>
        <input className={inputClass} required value={form.author} onChange={set('author')} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="ISBN" error={fieldErrors.isbn}>
          <input className={`${inputClass} font-mono`} required value={form.isbn} onChange={set('isbn')} placeholder="978-0-13-235088-4" />
        </Field>
        <Field label="Category" error={fieldErrors.category}>
          <input className={inputClass} required value={form.category} onChange={set('category')} placeholder="Programming" />
        </Field>
        <Field label="Publication year" error={fieldErrors.publicationYear}>
          <input className={inputClass} type="number" min={1450} max={2100} value={form.publicationYear} onChange={set('publicationYear')} />
        </Field>
        <Field label="Replacement cost (₹)" error={fieldErrors.replacementCost}>
          <input className={inputClass} type="number" min={0} step="0.01" value={form.replacementCost} onChange={set('replacementCost')} />
        </Field>
        {withCopies && (
          <Field label="Copies to add" error={fieldErrors.initialCopies}>
            <input className={inputClass} type="number" min={0} max={50} value={form.initialCopies} onChange={set('initialCopies')} />
          </Field>
        )}
      </div>
      <div className="flex justify-end gap-2 pt-2">
        {onCancel && <Button type="button" variant="secondary" onClick={onCancel}>Cancel</Button>}
        <Button type="submit" loading={busy}>{submitLabel}</Button>
      </div>
    </form>
  )
}
