# API reference

The interactive docs are in Swagger UI at `/swagger-ui.html` (click **Authorize** and paste a token). This page summarises every endpoint and the rules behind them.

## Try it in 2 minutes

With the app running, paste this into a terminal. It needs `jq`; run it on a fresh database, because the ISBN must be unique.

```bash
API=http://localhost:8080/api/v1
login() { curl -s -X POST $API/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r .accessToken; }
ADMIN=$(login admin@shelfspace.dev Admin@12345)
MEMBER=$(login member@shelfspace.dev Member@12345)

# Admin adds a book with one copy
BOOK=$(curl -s -X POST $API/books -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"title":"Clean Code","author":"Robert C. Martin","isbn":"978-0-13-235088-4","category":"Programming","publicationYear":2008,"replacementCost":599,"initialCopies":1}' | jq .id)

# Member searches, then requests it
curl -s "$API/books?q=clean" -H "Authorization: Bearer $MEMBER" | jq '.content[] | {title, availableCopies}'
LOAN=$(curl -s -X POST $API/loans -H "Authorization: Bearer $MEMBER" -H 'Content-Type: application/json' \
  -d "{\"bookId\": $BOOK}" | jq .id)

# Librarian approves; the due date is set from the member's policy
curl -s -X POST $API/loans/$LOAN/approve -H "Authorization: Bearer $ADMIN" | jq '{status, dueDate}'

# Member dashboard
curl -s $API/dashboard/me -H "Authorization: Bearer $MEMBER" | jq .summary

# Book comes back (a fine is charged automatically if late)
curl -s -X POST $API/loans/$LOAN/return -H "Authorization: Bearer $ADMIN" | jq '{status: .loan.status, overdueDays, fineCharged}'
```

## Authentication

ShelfSpace uses stateless JWT authentication.

| Endpoint | Auth | Description |
|---|---|---|
| `POST /api/v1/auth/register` | Public | Create a member account |
| `POST /api/v1/auth/login` | Public | Returns an access token (15 min) and a refresh token (7 days) |
| `POST /api/v1/auth/refresh` | Public | Swap a refresh token for a new pair. Each refresh token works once |
| `POST /api/v1/auth/logout` | Public | Ends the sign-in this refresh token belongs to |
| `GET /api/v1/auth/me` | Bearer token | Current user's profile |

Send the access token on every request as `Authorization: Bearer <token>`. In Swagger UI, click **Authorize** and paste the token.

## Your account

| Endpoint | Auth | Description |
|---|---|---|
| `PUT /api/v1/account/profile` | Bearer token | Update your name and phone (email can't be changed) |
| `POST /api/v1/account/password` | Bearer token | Change your password; needs the current one, and the new one must differ. Signs out other devices and returns new tokens for this one |
| `POST /api/v1/account/sign-out-everywhere` | Bearer token | Signs out every other device; returns new tokens for this one |

Changes are written to the audit log.

### Field rules

The server checks every field; the web app shows the same rules while typing, so a form it accepts won't be refused here. Errors come back as `400` with `fieldErrors`.

| Field | Rule |
|---|---|
| Password (sign-up, add member, change, reset) | 8-72 characters, an uppercase and a lowercase letter, a number, no space at the start or end. Common passwords are refused, even with numbers or symbols around them (`Password123!`, `Welcome1`); the list is in `backend/src/main/resources/validation/common-passwords.txt`. It also can't contain the person's name or the part of their email before the @ |
| Name | 2-100 characters after trimming, starting with a letter; letters in any language, spaces, apostrophes, hyphens and dots (`Zoë O'Brien-Nair`) |
| Email | Needs a domain with a dot and a 2+ letter ending (`name@gmail` is refused). Sign-in only needs an @, so older accounts still work |
| Phone | Optional; 10-15 digits, optionally starting with + |
| Book | Title required; author and category must contain a letter; ISBN-10 or ISBN-13 with a correct check digit; year from 1450 to next year; replacement cost 0-1,000,000 with at most 2 decimals |
| Reasons and notes | Required where asked (suspend, waive), up to 255 characters |

## Session security

- **Revocable tokens.** Every token carries the user's `token_version`. Changing a password, a librarian reset, suspension or "sign out other devices" bumps it, and every older token stops working on the next request (one primary-key lookup per request).
- **Refresh token rotation.** Each refresh token works once and is tracked in `refresh_tokens`. If a used one comes back, it was copied, so that whole sign-in is revoked. Expired rows are deleted nightly.
- **Sign-in lockout.** 5 wrong passwords for one email from one IP lock that pair for 15 minutes (`429` with `Retry-After`). Locking per email + IP stops guessing without letting a stranger lock the real owner out.
- **Per-IP limit.** Register, login and refresh are capped at 30 requests per IP per minute (`AUTH_RATE_LIMIT`, `0` turns it off).
- Counters live in memory, which suits a single instance. Running several would need a shared store such as Redis.

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

## Members (admin)

| Endpoint | Description |
|---|---|
| `GET /api/v1/members?q=&role=&status=&tier=` | Search by name, email or phone; each row shows active loans, overdue loans and unpaid fines (two grouped queries per page) |
| `GET /api/v1/members/{id}` | Profile with current loans, reservations and recent history |
| `POST /api/v1/members` | Add a member at the desk, or another librarian with `role: ADMIN` |
| `PUT /api/v1/members/{id}` | Update name, phone or membership tier |
| `POST /api/v1/members/{id}/suspend` (reason required) and `/activate` | A suspended member can't log in, borrow or reserve, but can still return books |
| `POST /api/v1/members/{id}/reset-password` | Set a new password for a member who forgot theirs (audited) |

## Audit log (admin)

Every librarian action, plus members' password changes and "sign out other devices", is written to `audit_log` in the same transaction as the change.

| Endpoint | Description |
|---|---|
| `GET /api/v1/audit?category=&action=&actorId=&entityType=&entityId=&from=&to=&page=` | Newest first. `category` is `BOOKS`, `LOANS`, `FINES`, `MEMBERS` or `RULES`; dates are inclusive. Each entry includes names (who did it, which book and member) looked up in one query per entity type, and deleted books keep their title |
| `GET /api/v1/audit/actions` | Action names for the filter menu |
| `GET /api/v1/audit/export?…` | Same filters as CSV (newest 10,000 rows). Cells starting with `=`, `+`, `-` or `@` are prefixed with `'` so spreadsheets don't run them as formulas |

## Borrowing rules

| Endpoint | Who | Description |
|---|---|---|
| `GET /api/v1/policies` and `/{tier}` | Logged in | Current rules per tier |
| `PUT /api/v1/policies/{tier}` | Admin | Replace a tier's rules, with range checks and `fineCap ≥ finePerDay`; the audit log records each changed field (`loanDays 14 -> 21`) |

Rule changes apply to new loans, renewals and fines. Due dates already set don't move.

## Wishlist and recommendations

| Endpoint | Who | Description |
|---|---|---|
| `GET /api/v1/wishlist` | Member | Saved books with live availability |
| `POST /api/v1/wishlist`, `DELETE /api/v1/wishlist/{bookId}` | Member | Save or remove a book (each book once) |
| `GET /api/v1/books/{id}/recommendations?limit=5` | Logged in | "Readers also borrowed", with each reason |
| `GET /api/v1/recommendations/me?limit=6` | Logged in | Personal suggestions from your borrowing history |

When a copy goes back on the shelf, and nobody is waiting for it in the reservation queue, members who wishlisted the book get a notification (at most once a day per book).

Recommendations use item-to-item co-occurrence: books borrowed by members who borrowed this one, ranked by how many members borrowed both. The counting is a single grouped SQL query. When there isn't enough history, they fall back to popular books in the same category, then popular overall, then the newest books. Personal suggestions never include books you've borrowed or saved.

## Default borrowing policies

| Tier | Max books | Loan days | Renewals | Fine/day | Fine cap | Blocked above |
|---|---|---|---|---|---|---|
| STUDENT | 5 | 14 | 2 | ₹10 | ₹500 | ₹100 unpaid |
| FACULTY | 10 | 30 | 3 | ₹5 | ₹500 | ₹200 unpaid |
| GUEST | 2 | 7 | 0 | ₹20 | ₹500 | ₹50 unpaid |
