# Contributing

## Ground rules

- **Never commit secrets.** All configuration comes from environment variables
  (`.env`, CI secrets). `.env` is git-ignored; commit only `.env.example`.
- **Keep the public API public.** Browsing, searching, and contributing must work
  with no account. Do not add authentication to the public `GET` endpoints.
- **Never fabricate scholarship.** Do not invent names, dates, biographies,
  attributions, or theology. Unknown values must be represented as unknown.
- **Respect image rights.** Contributor contact details stay private; rights holders
  can request correction or removal.

## Backend

```bash
cd EOTC_ICON-backend
mvn test        # runs against the in-memory H2 profile — no Docker required
mvn spring-boot:run
```

- Java 17, Spring Boot 4.0, Lombok.
- The schema is owned by Flyway. Never modify an already-applied migration; add a
  new `V{n}__*.sql`.
- Keep public DTOs free of private data (contributor contact email, internal notes).
  `PublicApiIntegrationTest` guards this.
- New public endpoints must be reflected in `SecurityConfig` and covered by a test.
- The PostgreSQL integration test is opt-in: set `EOTC_PG_IT_URL` (plus
  `EOTC_PG_IT_USER` / `EOTC_PG_IT_PASSWORD`) to enable it.
- See [`LOCAL_DEVELOPMENT.md`](LOCAL_DEVELOPMENT.md) for the no-Docker and the
  Docker Compose workflows.

## Frontend

```bash
cd EOTC_ICON-UI
npm start                       # ng serve (proxies /api to :8080)
npm run build                   # production build
npm test                        # unit tests
```

- Angular 19.2: standalone components, signals, and `@if`/`@for` control flow.
- Follow the design tokens in `src/styles.scss`; keep pages accessible (semantic
  landmarks, meaningful `alt` text, keyboard-reachable controls).
- Add unit tests for new services and non-trivial components.

## Pull requests

1. Branch from the default branch.
2. Keep changes focused; match existing style and conventions.
3. Ensure `mvn test` and `npm test` both pass, and that `npm run build` succeeds.
4. Describe the change and any schema or configuration impact.
5. Do not commit generated output (`target/`, `dist/`, `node_modules/`).
