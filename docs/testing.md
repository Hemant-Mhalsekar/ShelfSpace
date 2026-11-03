# Testing

## Backend

```bash
cd backend
./mvnw verify
```

| Layer | Examples |
|---|---|
| Unit | Fine maths, ISBN check digits, JWT signing/expiry/tampering, loan state transitions |
| Integration (Testcontainers MySQL) | Auth flow, catalog rules, every borrowing rule, reservation queue and hold expiry, jobs, dashboards |
| Concurrency | 10 threads request the last copy at once; exactly one succeeds |

Integration tests share one MySQL container for the whole run (the singleton container pattern), so the suite stays fast. They're skipped automatically when Docker isn't running.

## Frontend

```bash
cd frontend && npm test
```

47 component tests with Vitest and React Testing Library, run in jsdom against a fake API (MSW), so no backend is needed. Any request a test didn't mock fails the test.

| Area | Examples |
|---|---|
| API client | Token sent on every call, silent refresh on 401, one shared refresh for parallel requests, sign-out when refresh fails, another tab's refresh is picked up |
| Sign in | Success, wrong password, lockout message, demo buttons |
| Account | Membership limits in words, Save only when changed, password rules and mismatch, wrong current password, new tokens kept after a change, sign out other devices |
| Activity | Entries read as sentences with links, own-account wording, details formatting, filter by person and category |
| Member profile | Check in, with on-time and late-fine messages |
| Field rules | Every password, name, email, phone, ISBN, year and cost rule; sign-up form shows problems after leaving a field, fixes them live, and doesn't send until everything passes |

## End to end

```bash
docker compose up -d --build
cd e2e && npm ci && npx playwright install chromium && npm test
```

One Playwright test drives a real Chromium through the whole stack (nginx-served web app, API, MySQL), as two people at once:

1. The librarian adds a book with one copy.
2. A new member signs up; the password rules are checked as they type.
3. The member finds the book and asks to borrow it; My Books shows "Awaiting approval".
4. The librarian approves it on the loan desk.
5. The member sees it checked out with a due date.
6. The librarian checks it in from the member's profile.
7. The activity log shows both steps as sentences.

Each run uses a new book and member, so it can run again on the same database. In CI the job builds both Docker images, starts them with `docker compose`, runs the test, and uploads screenshots and a trace if it fails.

All three suites run in GitHub Actions on every push and pull request.
