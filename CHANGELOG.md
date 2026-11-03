# Changelog

## v1.1.0 (November 2026)

Live demo, a redesigned web app, account and session security, an activity log, field validation everywhere, and tests on every layer.

### New
- **Live demo** at https://shelf-space-pi.vercel.app, hosted free on Vercel (web), Render (API) and Aiven (MySQL); a notice explains the first slow load while the free API wakes up
- **Want to Read list** with back-in-stock alerts, and **"Readers also borrowed"** recommendations on book pages and My Books
- **Account page:** edit name and phone, change password, see your membership limits in plain words, sign out of other devices
- **Librarian password reset** from a member's profile
- **Activity log** (`/admin/activity`): every change as a readable sentence, filter by area, action, person and date, export to CSV
- **Check in** a book straight from a member's profile
- Members' own renewals now show in the activity log

### Security
- Tokens can be revoked: a password change, librarian reset, suspension or "sign out other devices" ends every older session at once
- Refresh tokens are single-use and tracked; a reused one ends that sign-in, and logout now tells the server
- Sign-in lockout after 5 wrong passwords per email and IP for 15 minutes, plus a per-IP limit on sign-in, sign-up and refresh
- Stronger passwords: upper and lowercase letters and a number, common passwords refused, can't contain your name or email
- CSV export guards against spreadsheet formula injection

### Improved
- **Redesigned web app** in an Apple Books style, with plain title covers and a tab bar for members on phones
- **Field checks as you type** on every form (names, emails, phones, ISBNs, years, costs), using the same rules and messages as the server, with a password strength bar
- Form errors are linked to their fields for screen readers
- Server times use the library's time zone (`-Duser.timezone`)

### Tests and tooling
- 157 backend tests (was 88), 47 frontend component tests (Vitest, React Testing Library, MSW) and a Playwright end-to-end test of the full borrowing journey, all in CI
- The end-to-end job builds both Docker images and runs the test against `docker compose`

### Project
- The API now lives in `backend/`, next to `frontend/` and `e2e/`
- Shorter README with screenshots; reference material moved to `docs/` (API, architecture, testing, deployment, development)
- Flyway migrations V4 (wishlist), V5 (session security) and V6 (audit indexes)

### Upgrade notes
- Render: set **Root Directory** to `backend`
- Everyone signs in again once after deploying, because refresh tokens are now tracked
- New passwords must meet the stronger rules; existing passwords keep working

## v1.0.0 (November 2026)

First release: a full-stack library management system.

### Backend (Spring Boot API)
- **Auth:** JWT access and refresh tokens, BCrypt passwords, ADMIN and MEMBER roles, demo accounts
- **Catalog:** books and individual copies with generated barcodes, ISBN-10/13 validation, paged search with filters, soft delete
- **Borrowing:** request → approve → issue → renew → return or lost, with every rule read from per-tier borrowing policies
- **Concurrency:** optimistic locking stops two members holding the same copy (tested with 10 parallel requests)
- **Fines:** per-day overdue fines with a cap, lost-book charges, pay or waive (with a reason)
- **Reservations:** FIFO waiting list, 48-hour holds, queue position and estimated date
- **Background jobs:** overdue marking, due-tomorrow reminders, hold expiry
- **Notifications:** in-app alerts with unread counts
- **Dashboards:** admin stats (top books, 6-month trend) and a member summary
- **Admin:** member management (tiers, suspend/reactivate) and editable borrowing rules
- **Audit log** of every admin action
- Flyway migrations V1–V3; 88 tests, including Testcontainers MySQL integration tests

### Frontend (React web app)
- Member screens: catalog, book page (borrow/reserve), dashboard, notifications, fines
- Librarian screens: dashboard, loan desk, books, members, fines, borrowing rules
- React 19, TypeScript, Tailwind CSS 4, TanStack Query, React Router

### Ops
- Docker: `docker compose up --build` runs MySQL, the API and the web app (nginx)
- GitHub Actions CI: backend tests, frontend lint and build, both Docker images
