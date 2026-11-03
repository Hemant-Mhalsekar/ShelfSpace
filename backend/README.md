# ShelfSpace API

Spring Boot 3.3 REST API (Java 21, MySQL, Flyway, JWT). See the [main README](../README.md) for the overview.

```bash
docker compose up -d mysql   # from the repo root
./mvnw spring-boot:run       # http://localhost:8080, Swagger UI at /swagger-ui.html
./mvnw test                  # unit + Testcontainers integration tests (needs Docker)
```

- Code: `src/main/java/com/shelfspace/<feature>`, one package per feature (entity, repository, service, controller, `dto/`)
- Database changes: a new Flyway file in `src/main/resources/db/migration`
- Settings: environment variables listed in `.env.example`, read by `src/main/resources/application.yml`

More: [API reference](../docs/api.md) · [Architecture](../docs/architecture.md) · [Testing](../docs/testing.md)
