# End-to-end tests

One Playwright test that drives a real browser through the whole app (web, API and MySQL):
a librarian adds a book, a new member signs up and borrows it, the librarian approves it,
the member sees the due date, the librarian checks it in, and the activity log shows it.

```bash
docker compose up -d --build          # from the repo root; wait until http://localhost:3000 loads
cd e2e
npm ci
npx playwright install chromium
npm test
```

Each run uses a new book and member, so it can run again on the same database.
Set `E2E_BASE_URL` to test another address, e.g. `E2E_BASE_URL=http://localhost:5173 npm test` with the dev server.
On failure, `npm run report` opens screenshots and a step-by-step trace.
