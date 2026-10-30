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
- [ ] Week 2: borrow, return and renew; fines; concurrency test
- [ ] Week 3: reservations, overdue job, email reminders, dashboards
- [ ] Week 4: Docker image, GitHub Actions CI, deployment

## Branching

- `main`: stable releases only
- `develop`: integration branch
- `feature/*`: one branch per feature, merged into `develop` with a pull request
