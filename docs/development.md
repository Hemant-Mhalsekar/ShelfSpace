# Development

## Running from source

You need JDK 21, plus Docker for the database and tests:

```bash
docker compose up -d mysql   # from the repo root
cd backend
./mvnw spring-boot:run       # API on http://localhost:8080
./mvnw test                  # 156 tests; integration tests start their own MySQL
```

> JDK 22+ isn't supported yet: Lombok, used by Spring Boot 3.3, fails to compile on it. With jEnv, `backend/.java-version` selects 21 automatically. `backend/.env.example` lists the environment variables the API reads.

Useful URLs while it runs:

- Swagger UI: http://localhost:8080/swagger-ui.html (click **Authorize** and paste a token)
- Health: http://localhost:8080/actuator/health

## Frontend

See [`frontend/README.md`](../frontend/README.md). In short: `cd frontend && npm install && npm run dev` (http://localhost:5173), with the API on port 8080.

## Branching

- `main`: released versions
- `develop`: integration branch (default)
- `feature/*`: one per feature, merged into `develop` through a pull request after CI passes
