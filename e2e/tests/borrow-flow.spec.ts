import { expect, test, type Browser, type Page } from '@playwright/test'

/**
 * The main journey, through the real UI, API and database:
 * a librarian adds a book, a new member signs up and borrows it, the librarian approves it,
 * the member sees the due date, the librarian checks it back in, and it shows in the activity log.
 */

const run = Date.now().toString(36)
const book = { title: `E2E Book ${run}`, author: 'Test Author', isbn: randomIsbn13(), category: 'Testing' }
const reader = { name: 'Asha Rao', email: `e2e-${run}@example.com`, password: 'Shelf2Read-Daily' }
const LIBRARIAN = { email: 'admin@shelfspace.dev', password: 'Admin@12345' }

test.describe.configure({ mode: 'serial' })

let librarian: Page
let member: Page

test.beforeAll(async ({ browser }) => {
  librarian = await signIn(browser, LIBRARIAN.email, LIBRARIAN.password)
  member = await (await browser.newContext()).newPage()
})

test.afterAll(async () => {
  await librarian.context().close()
  await member.context().close()
})

test('librarian adds a book with one copy', async () => {
  await librarian.goto('/admin/books')
  await librarian.getByRole('button', { name: '+ Add book' }).click()
  const form = librarian.getByRole('dialog', { name: 'Add a book' })
  await form.getByLabel('Title').fill(book.title)
  await form.getByLabel('Author').fill(book.author)
  await form.getByLabel('ISBN').fill(book.isbn)
  await form.getByLabel('Category').fill(book.category)
  await form.getByLabel('Publication year').fill('2020')
  await form.getByLabel('Copies to add').fill('1')
  await form.getByRole('button', { name: 'Add book' }).click()

  await expect(librarian.getByText(`Added “${book.title}” with 1 copy`)).toBeVisible()
})

test('a new member signs up, with the password rules checked as they type', async () => {
  await member.goto('/register')
  await member.getByLabel('Full name').fill(reader.name)
  await member.getByLabel('Email').fill(reader.email)

  const password = member.getByLabel('Password')
  await password.fill('Password1')
  await password.blur()
  await expect(member.getByText('Too common. Choose something harder to guess')).toBeVisible()

  await password.fill(reader.password)
  await expect(member.getByText('Strong')).toBeVisible()
  await member.getByRole('button', { name: 'Create Account' }).click()

  await expect(member).toHaveURL(/\/books$/)
})

test('the member finds the book and asks to borrow it', async () => {
  await member.getByPlaceholder('Titles, authors or ISBN').fill(book.title)
  await member.getByRole('link', { name: new RegExp(book.title) }).first().click()
  await expect(member.getByRole('heading', { name: book.title })).toBeVisible()

  await member.getByRole('button', { name: 'Borrow', exact: true }).click()
  await expect(member.getByText('Request sent!')).toBeVisible()

  await member.goto('/dashboard')
  await expect(tile(member, book.title)).toContainText('Awaiting approval')
})

test('the librarian approves the request on the loan desk', async () => {
  await librarian.goto('/admin/loans')
  const row = librarian.locator('div.px-4', { hasText: book.title }).filter({ has: librarian.getByRole('button', { name: 'Approve' }) })
  await expect(row).toContainText(reader.name)
  await row.getByRole('button', { name: 'Approve' }).click()

  await expect(librarian.getByText(`Issued “${book.title}” to ${reader.name}`)).toBeVisible()
})

test('the member sees it checked out with a due date', async () => {
  await member.reload()
  await expect(tile(member, book.title)).toContainText(/Due /)
  await expect(member.getByText('To return a book, bring it to the library desk.', { exact: false })).toBeVisible()
})

test("the librarian checks it in from the member's profile", async () => {
  await librarian.goto('/admin/members')
  await librarian.getByPlaceholder(/Search/).fill(reader.email)
  await librarian.getByRole('link', { name: reader.name }).first().click()

  await expect(librarian.getByRole('heading', { name: 'Current loans' })).toBeVisible()
  await expect(librarian.getByRole('main').getByText(book.title)).toBeVisible()
  await librarian.getByRole('button', { name: 'Check in' }).click()

  await expect(librarian.getByText('Returned on time. No fine.')).toBeVisible()
  await expect(librarian.getByText('Nothing borrowed')).toBeVisible()
})

test('the activity log tells the story in sentences', async () => {
  await librarian.goto('/admin/activity?category=LOANS')
  const entries = librarian.getByRole('listitem').filter({ hasText: book.title })
  await expect(entries.filter({ hasText: 'checked in' })).toHaveCount(1)
  await expect(entries.filter({ hasText: 'approved a loan of' })).toContainText(`Library Admin approved a loan of ${book.title} for ${reader.name}`)
})

// ---------- helpers ----------

async function signIn(browser: Browser, email: string, password: string) {
  const page = await (await browser.newContext()).newPage()
  await page.goto('/login')
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.getByRole('button', { name: 'Sign Out' })).toBeVisible()
  return page
}

/** A book on the member's My Books shelf. */
function tile(page: Page, title: string) {
  return page.locator('a, div', { hasText: title }).filter({ has: page.getByText(/Awaiting approval|Due /) }).last()
}

/** A valid ISBN-13 that's different every run, so the test can run again on the same database. */
function randomIsbn13() {
  const digits = '978' + Array.from({ length: 9 }, () => Math.floor(Math.random() * 10)).join('')
  const sum = [...digits].reduce((s, d, i) => s + Number(d) * (i % 2 ? 3 : 1), 0)
  return digits + ((10 - (sum % 10)) % 10)
}
