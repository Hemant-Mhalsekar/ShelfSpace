# Development

## Running from source

To run from source instead, you need JDK 21, plus Docker for the database and tests:

```bash
docker compose up -d mysql
./mvnw spring-boot:run      # or: mvn spring-boot:run
./mvnw test                 # 134 tests; integration tests start their own MySQL
```

> JDK 22+ isn't supported yet: Lombok, used by Spring Boot 3.3, fails to compile on it. With jEnv, the included `.java-version` selects 21 automatically.

Useful URLs while it runs:

- Swagger UI: http://localhost:8080/swagger-ui.html (click **Authorize** and paste a token)
- Health: http://localhost:8080/actuator/health

## Frontend

See [`frontend/README.md`](../frontend/README.md). In short: `cd frontend && npm install && npm run dev` (http://localhost:5173), with the API on port 8080.

## Branching

- `main`: released versions
- `develop`: integration branch (default)
- `feature/*`: one per feature, merged into `develop` through a pull request after CI passes
