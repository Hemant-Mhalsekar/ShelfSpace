# Deploying (free: Vercel + Render + Aiven)

| Part | Host | Notes |
|---|---|---|
| Web app | **Vercel** | `frontend/vercel.json` forwards `/api` to the API, so the browser sees one origin |
| API | **Render** (free web service, Docker) | Sleeps after 15 minutes idle; waking up takes 2–3 minutes (the login page shows a "waking up" notice) |
| MySQL | **Aiven** (free MySQL) | 1 GB storage; SSL required |

1. **Aiven:** create a free MySQL service and note its host, port, user, password and database (`defaultdb`).
2. **Render:** **New → Web Service →** this repo, runtime **Docker**, branch `main`, health check path `/actuator/health`. Environment:

   | Variable | Value |
   |---|---|
   | `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | from Aiven |
   | `DB_PARAMS` | `?sslMode=REQUIRED` |
   | `DB_POOL_SIZE` | `5` |
   | `JWT_SECRET` | output of `openssl rand -base64 48` |
   | `SEED_ENABLED` | `true` (creates the demo accounts) |
   | `AUTH_RATE_LIMIT` | optional: sign-in/register/refresh requests per IP per minute (default `30`) |
   | `JAVA_OPTS` | `-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -Duser.timezone=Asia/Kolkata` (fits the 512 MB free instance; the time zone is the library's local time, used for due dates, jobs and timestamps) |

3. **Vercel:** set the Render URL in `frontend/vercel.json`, then **Add New → Project →** this repo with **Root Directory** `frontend` (Vite is detected automatically).

Other hosts work the same way: the API reads everything from environment variables and listens on `$PORT`. `docker compose up --build` runs the whole stack on any machine with Docker.

## Demo accounts

Created automatically on startup when `SEED_ENABLED=true` (the default for local runs):

| Role | Email | Password |
|---|---|---|
| Admin | `admin@shelfspace.dev` | `Admin@12345` |
| Member | `member@shelfspace.dev` | `Member@12345` |

Set `SEED_ENABLED=false` and a strong `JWT_SECRET` in any real deployment.
