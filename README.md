# ShelfSpace

A library management backend built with Java and Spring Boot. It handles the book catalog, members, borrowing and returns, reservations, and fines, with separate permissions for librarians and members.

> Status: in progress. See the [roadmap](#roadmap).

## Highlights

- **Book vs copy model.** Each physical copy is tracked by barcode and status (available, on hold, borrowed, lost, damaged).
- **Loan state machine.** `REQUESTED → APPROVED → ISSUED → OVERDUE/RETURNED/LOST`, and invalid jumps are rejected.
- **No double-booking.** Optimistic locking (`@Version`) on copies stops two members borrowing the last copy at once.
- **Rules as data.** Loan length, book limits and fine rates live in a `borrowing_policies` table per membership tier.
- **Reservation queue.** First come, first served, with a 48-hour hold when a copy comes back.
- **Fine ledger.** Fines can be paid or waived, are capped, and block borrowing above a threshold.
- **Audit log** of admin actions.

## Tech stack

Java 17 · Spring Boot 3.3 · Spring Security (JWT) · Spring Data JPA · MySQL 8 · Flyway · springdoc OpenAPI · JUnit 5 · Testcontainers · Docker

## Project structure

Packages are split by feature, so this is a modular monolith:

```
com.shelfspace
├── auth          # login, register, JWT
├── member        # users, roles, membership tiers
├── catalog       # books and physical copies
├── circulation   # loans: borrow, return, renew
├── reservation   # waiting list and holds
├── fine          # fine ledger
├── policy        # borrowing rules per tier
├── notification  # reminders
├── audit         # audit log
└── common        # config, base entity, error handling
```

## Data model

```
users ──< loans >── book_copies >── books
  │         │                        │
  │         └──< fines               │
  └──────< reservations >────────────┘
borrowing_policies (one row per membership tier)
```

## Running locally

Prerequisites: **JDK 21** (or 17), Maven, Docker.

> Newer JDKs (22+) are not supported yet: Lombok, used by Spring Boot 3.3, fails to compile on them.
> On macOS with Homebrew: `brew install openjdk@21` and `export JAVA_HOME=/opt/homebrew/opt/openjdk@21`.

```bash
docker compose up -d          # starts MySQL on port 3306
mvn spring-boot:run           # Flyway creates the tables on startup
```

- API docs: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Run the tests with `mvn test`. Integration tests use Testcontainers and are skipped if Docker isn't running.

## Authentication

ShelfSpace uses stateless JWT authentication.

| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/v1/auth/register` | Public | Create a member account |
| `POST /api/v1/auth/login` | Public | Returns an access token (15 min) and a refresh token (7 days) |
| `POST /api/v1/auth/refresh` | Public | Swap a refresh token for a new token pair |
| `GET /api/v1/auth/me` | Bearer token | Current user's profile |

Send the access token on every request as `Authorization: Bearer <token>`. In Swagger UI, click **Authorize** and paste the token.

## Catalog

| Endpoint | Who | Description |
|---|---|---|
| `GET /api/v1/books?q=&category=&availableOnly=&page=&size=&sort=` | Logged in | Search by title, author or ISBN. Paged; sortable by title, author, category, publicationYear, createdAt |
| `GET /api/v1/books/{id}` | Logged in | Book details with total and available copies |
| `GET /api/v1/books/categories` | Logged in | All categories |
| `POST /api/v1/books` | Admin | Add a book, optionally with `initialCopies` |
| `PUT /api/v1/books/{id}` | Admin | Update a book |
| `DELETE /api/v1/books/{id}` | Admin | Soft delete; refused while copies are borrowed/on hold or members are waiting |
| `GET /api/v1/books/{id}/copies` | Admin | List physical copies |
| `POST /api/v1/books/{id}/copies` | Admin | Add copies; barcodes are generated (`SS000012-003`) |
| `PATCH /api/v1/copies/{id}/status` | Admin | Mark a copy AVAILABLE, DAMAGED or LOST |

Design notes:
- **ISBN validation** checks the ISBN-10/13 check digit and stores ISBNs without hyphens, so `978-0-13-235088-4` and `9780132350884` count as the same book.
- **No N+1 queries:** copy counts for a whole search page come from one grouped query.
- **Copy states owned by circulation:** `BORROWED` and `ON_HOLD` are only set by the loan and reservation flows, never by hand.
- **Audit trail:** every admin change is written to `audit_log` in the same transaction.

## Borrowing and fines

```
Member requests ─▶ REQUESTED (copy ON_HOLD)
                    ├─ member cancels ─▶ CANCELLED
                    ├─ admin rejects  ─▶ REJECTED
                    └─ admin approves ─▶ ISSUED (copy BORROWED, due date set)
                                          ├─ renew  ─▶ due date + loan period
                                          ├─ return ─▶ RETURNED (+ overdue fine)
                                          └─ lost   ─▶ LOST (+ replacement cost)
```

| Endpoint | Who | Description |
|---|---|---|
| `POST /api/v1/loans` | Member | Request a book; one copy is held |
| `GET /api/v1/loans/me?status=` | Member | My current loans and history, with live overdue days and fine |
| `GET /api/v1/loans/{id}` | Owner/Admin | One loan (others get 404) |
| `POST /api/v1/loans/{id}/cancel` | Owner | Cancel a pending request |
| `POST /api/v1/loans/{id}/renew` | Owner/Admin | Extend the due date |
| `GET /api/v1/loans?memberId=&status=` | Admin | All loans; `status=REQUESTED` is the approval queue |
| `POST /api/v1/loans/{id}/approve` and `/reject` | Admin | Decide a request |
| `POST /api/v1/loans/{id}/return` and `/lost` | Admin | Close a loan and charge fines |
| `GET /api/v1/fines/me` | Member | My fines and total owed |
| `GET /api/v1/fines?memberId=&status=` | Admin | All fines |
| `POST /api/v1/fines/{id}/pay` and `/waive` | Admin | Collect or waive (waive needs a reason) |

**Borrowing rules**, checked in order, all read from the member's tier in `borrowing_policies`:
1. Account is active.
2. No books past their due date.
3. Unpaid fines are not above the tier limit.
4. Active loans are under the tier's book limit.
5. They don't already have this title.
6. A copy is available.

**Renewals:** only while issued and not overdue, under the renewal limit, and when nobody is waiting for the book.

**Fines:** `days late × fine per day`, capped per tier. A lost book adds its replacement cost.

**Concurrency:** requesting a book holds a specific copy. `book_copies.version` (`@Version`) makes the second of two simultaneous updates fail, which returns 409. `LoanConcurrencyIntegrationTest` fires 10 requests at the last copy at once and asserts exactly one succeeds.

**Testable time:** all date logic uses an injected `java.time.Clock`.

## Reservations

```
No copy available → reserve → WAITING (#1, #2 …)
Copy freed → next in line → READY (copy ON_HOLD for 48h, member notified)
   ├─ borrowed in time → FULFILLED
   ├─ hold runs out    → EXPIRED, copy passes to the next person
   └─ cancelled        → CANCELLED, copy passes to the next person
```

| Endpoint | Who | Description |
|---|---|---|
| `POST /api/v1/reservations` | Member | Join the queue (only when no copy is available) |
| `GET /api/v1/reservations/me` | Member | My reservations with queue position and estimated date |
| `GET /api/v1/reservations/{id}` | Owner/Admin | One reservation |
| `POST /api/v1/reservations/{id}/cancel` | Owner/Admin | Leave the queue or release a held copy |

Every path that frees a copy (return, reject, cancel, new copies, a repaired copy, an expired hold) goes through one method, `ReservationService.releaseCopy`. It gives the copy to the next member in the queue, so a member who isn't next can never take it. The limits (`max_reservations`, `hold_hours`) are set per tier in `borrowing_policies`.

## Background jobs

| Job | Default schedule | What it does |
|---|---|---|
| Overdue check | 00:05 daily | `ISSUED` loans past due become `OVERDUE`, and the member is notified |
| Due-tomorrow reminders | 09:00 daily | One reminder per loan per day, even if the job runs twice |
| Hold expiry | Every 15 min | Uncollected holds expire and pass the copy on |

Schedules are set under `app.jobs.*`. Set `SCHEDULING_ENABLED=false` to turn the jobs off; tests do this and call the jobs directly.

## Notifications and dashboards

| Endpoint | Who | Description |
|---|---|---|
| `GET /api/v1/notifications/me?unreadOnly=` | Logged in | Alerts: approved/rejected, due soon, overdue, reservation ready, hold expired |
| `GET /api/v1/notifications/me/unread-count` | Logged in | Number of unread alerts |
| `POST /api/v1/notifications/{id}/read`, `/me/read-all` | Logged in | Mark one or all as read |
| `GET /api/v1/dashboard/me` | Member | Books out vs allowed, due soon, overdue, fines, reservations, recent returns |
| `GET /api/v1/dashboard/admin` | Admin | Inventory, circulation, fines, top 5 books, borrowing over the last 6 months |

All notifications go through `NotificationService`, so adding email later only touches that class.

### Demo accounts

Created automatically on startup when `SEED_ENABLED=true` (the default for local runs):

| Role | Email | Password |
|---|---|---|
| Admin | `admin@shelfspace.dev` | `Admin@12345` |
| Member | `member@shelfspace.dev` | `Member@12345` |

Set `SEED_ENABLED=false` and a strong `JWT_SECRET` in any real deployment.

## Default borrowing policies

| Tier | Max books | Loan days | Renewals | Fine/day | Fine cap | Blocked above |
|---|---|---|---|---|---|---|
| STUDENT | 5 | 14 | 2 | ₹10 | ₹500 | ₹100 unpaid |
| FACULTY | 10 | 30 | 3 | ₹5 | ₹500 | ₹200 unpaid |
| GUEST | 2 | 7 | 0 | ₹20 | ₹500 | ₹50 unpaid |

## Roadmap

- [x] Project setup, schema, entities, error handling, Swagger
- [x] Week 1: JWT auth (register, login, refresh, me), demo accounts
- [x] Week 1: book and copy CRUD, catalog search
- [x] Week 2: borrow, return and renew; fines; concurrency test
- [x] Week 3: reservations, overdue job, reminders (in-app), dashboards
- [ ] Week 4: Docker image, GitHub Actions CI, deployment

## Branching

- `main`: stable releases only
- `develop`: integration branch
- `feature/*`: one branch per feature, merged into `develop` with a pull request
