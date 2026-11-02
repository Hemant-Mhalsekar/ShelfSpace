import { useState, type FormEvent } from 'react'
import { ApiError } from '../../api/client'
import type { Book, BookInput } from '../../api/types'
import { Alert, Button, Field } from '../../components/ui'
import { errorMessage, inputClass } from '../../lib/ui-helpers'
import { useFieldChecks } from '../../lib/useFieldChecks'
import { costProblem, isbnProblem, requiredText, wholeNumberProblem, yearProblem } from '../../lib/validation'

const NEXT_YEAR = new Date().getFullYear() + 1

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
  const [busy, setBusy] = useState(false)
  const checks = useFieldChecks({
    title: requiredText(form.title, 'the title'),
    author: requiredText(form.author, 'the author', { needsLetter: true }),
    isbn: isbnProblem(form.isbn),
    category: requiredText(form.category, 'a category', { needsLetter: true, max: 100 }),
    publicationYear: yearProblem(form.publicationYear),
    replacementCost: costProblem(form.replacementCost),
    initialCopies: withCopies ? wholeNumberProblem(form.initialCopies, 0, 50) : null,
  })

  const set = (k: keyof typeof form) => (e: { target: { value: string } }) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!checks.ready()) return
    setBusy(true)
    try {
      await onSubmit({
        title: form.title.trim(),
        author: form.author.trim(),
        isbn: form.isbn.trim(),
        category: form.category.trim(),
        publicationYear: form.publicationYear ? Number(form.publicationYear) : null,
        replacementCost: form.replacementCost ? Number(form.replacementCost) : null,
        ...(withCopies ? { initialCopies: Number(form.initialCopies || 0) } : {}),
      })
    } catch (err) {
      if (err instanceof ApiError) checks.setServerErrors(err.fieldErrors)
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      {error && <Alert>{error}</Alert>}
      <Field label="Title" error={checks.error('title')}>
        <input className={inputClass} required value={form.title} onChange={set('title')} {...checks.field('title')} />
      </Field>
      <Field label="Author" error={checks.error('author')}>
        <input className={inputClass} required value={form.author} onChange={set('author')} {...checks.field('author')} />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="ISBN" error={checks.error('isbn')}>
          <input className={`${inputClass} font-mono`} required value={form.isbn} onChange={set('isbn')} {...checks.field('isbn')} placeholder="978-0-13-235088-4" />
        </Field>
        <Field label="Category" error={checks.error('category')}>
          <input className={inputClass} required value={form.category} onChange={set('category')} {...checks.field('category')} placeholder="Programming" />
        </Field>
        <Field label="Publication year" error={checks.error('publicationYear')}>
          <input className={inputClass} type="number" min={1450} max={NEXT_YEAR} value={form.publicationYear} onChange={set('publicationYear')} {...checks.field('publicationYear')} />
        </Field>
        <Field label="Replacement cost (₹)" error={checks.error('replacementCost')}>
          <input className={inputClass} type="number" min={0} max={1000000} step="0.01" value={form.replacementCost} onChange={set('replacementCost')} {...checks.field('replacementCost')} />
        </Field>
        {withCopies && (
          <Field label="Copies to add" error={checks.error('initialCopies')}>
            <input className={inputClass} type="number" min={0} max={50} value={form.initialCopies} onChange={set('initialCopies')} {...checks.field('initialCopies')} />
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
