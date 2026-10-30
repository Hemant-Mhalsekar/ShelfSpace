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

Prerequisites: JDK 17+, Maven, Docker.

```bash
docker compose up -d          # starts MySQL on port 3306
mvn spring-boot:run           # Flyway creates the tables on startup
```

- API docs: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Run the tests with `mvn test`. Integration tests use Testcontainers and are skipped if Docker isn't running.

## Default borrowing policies

| Tier | Max books | Loan days | Renewals | Fine/day | Fine cap | Blocked above |
|---|---|---|---|---|---|---|
| STUDENT | 5 | 14 | 2 | ₹10 | ₹500 | ₹100 unpaid |
| FACULTY | 10 | 30 | 3 | ₹5 | ₹500 | ₹200 unpaid |
| GUEST | 2 | 7 | 0 | ₹20 | ₹500 | ₹50 unpaid |

## Roadmap

- [x] Project setup, schema, entities, error handling, Swagger
- [ ] Week 1: JWT auth (register, login, refresh), book and copy CRUD
- [ ] Week 2: borrow, return and renew; fines; concurrency test
- [ ] Week 3: reservations, overdue job, email reminders, dashboards
- [ ] Week 4: Docker image, GitHub Actions CI, deployment

## Branching

- `main`: stable releases only
- `develop`: integration branch
- `feature/*`: one branch per feature, merged into `develop` with a pull request
