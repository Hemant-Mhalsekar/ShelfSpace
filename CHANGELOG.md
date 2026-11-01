# Changelog

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
