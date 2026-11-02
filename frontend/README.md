# ShelfSpace web

React frontend for the ShelfSpace API.

**Stack:** Vite · React 19 · TypeScript · Tailwind CSS 4 · React Router · TanStack Query

## Run it

You need Node 22+ and the API running on `localhost:8080` (`docker compose up` from the repo root, or `./mvnw spring-boot:run`).

```bash
npm install
npm run dev        # http://localhost:5173
```

Vite forwards `/api` to `localhost:8080`, so there's no CORS setup. Log in with one of the demo buttons on the login page.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Type-check (`tsc`) and production build into `dist/` |
| `npm run lint` | Lint with oxlint |
| `npm test` | Component tests (Vitest + React Testing Library, mocked API via MSW) |
| `npm run test:watch` | Same, re-running on every save |
| `npm run preview` | Serve the production build locally |

## Structure

```
src/
├── api/          # fetch wrapper (JWT + silent refresh), typed endpoints, API types
├── auth/         # auth context, useAuth hook
├── components/   # layout, route guard, small UI kit (Button, Card, Badge…)
├── lib/          # formatting and UI helpers
└── pages/        # one file per screen
```

## Notes

- Server data is fetched and cached with TanStack Query; actions invalidate the affected queries, so screens update without manual refreshes.
- Access tokens last 15 minutes. On a `401`, the client refreshes once (shared across concurrent requests) and retries; if that fails, you're logged out.
- Catalog filters live in the URL, so searches can be bookmarked and the back button works.
