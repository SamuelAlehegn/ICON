# EOTC Sacred Icons Archive

A production-oriented web application for documenting, discovering, and preserving
**Ethiopian Orthodox Tewahedo Church (EOTC) icons and religious wall paintings**.

Every record captures *what is depicted*, *where the artwork is located*, and *who
contributed the photograph* (subject to the contributor's attribution preference).
The entire published archive is browsable, searchable, and viewable **without any
login**. Sign-in is only needed for optional features (tracking your own
submissions) and for moderators.

> The archive is inspired by the cataloguing and preservation goals of *Mazgaba
> Se'elet*, a photographic database of Ethiopian art. This project is independent:
> it reuses only publicly known information and reproduces no other archive's
> images, content, or code.

---

## Architecture

| Layer     | Technology                                                        |
|-----------|-------------------------------------------------------------------|
| Frontend  | Angular 19 (standalone components, signals, Angular Material, SCSS) |
| Backend   | Spring Boot 3.5 (Java 21), Spring Security (JWT), Spring Data JPA |
| Database  | PostgreSQL 16, schema managed by Flyway migrations                |
| Storage   | Pluggable: local filesystem (default) or any S3-compatible store (MinIO) |
| API docs  | springdoc-openapi (Swagger UI)                                    |

```
Angular SPA ──HTTP──▶ Spring Boot REST API ──JPA──▶ PostgreSQL
                            │
                            ├──▶ Local media (./data/media)  OR
                            └──▶ S3 / MinIO (object storage)
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for design details and
[`docs/API.md`](docs/API.md) for the endpoint reference.

---

## Quick start (Docker)

Requires Docker Desktop.

```bash
cp .env.example .env          # then edit secrets (JWT_SECRET, passwords)
docker compose up --build
```

| Service          | URL                                      |
|------------------|------------------------------------------|
| Web app          | http://localhost:8081                    |
| REST API         | http://localhost:8080/api/v1             |
| Swagger UI       | http://localhost:8080/swagger-ui.html    |
| MinIO console    | http://localhost:9001                    |
| PostgreSQL       | localhost:5432                           |

On first boot the schema is created by Flyway. If `ADMIN_EMAIL`/`ADMIN_PASSWORD`
are set, an administrator account is created. Set `SEED_SAMPLE_DATA=true` to load
clearly-marked sample records with placeholder images.

---

## Local development

### Backend

The `local` Spring profile runs entirely against an **in-memory H2 database** in
PostgreSQL-compatibility mode — no PostgreSQL or Docker required. It uses its own
Flyway migration set (`src/main/resources/db/h2`) and seeds clearly-marked sample
data by default. The data is discarded when the app stops.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local \
JWT_SECRET=dev-secret-at-least-32-bytes-long-xxxx \
mvn spring-boot:run
```

To run against a real PostgreSQL instead, omit the profile and provide connection
details (the default configuration is PostgreSQL, matching production):

```bash
cd backend
DB_URL=jdbc:postgresql://localhost:5432/eotc_archive \
DB_USERNAME=eotc DB_PASSWORD=eotc \
JWT_SECRET=dev-secret-at-least-32-bytes-long-xxxx \
mvn spring-boot:run
```

Run tests (uses Testcontainers with PostgreSQL, so Docker must be running):

```bash
mvn test
```

### Frontend

```bash
cd frontend
npm install
npm start          # ng serve with proxy to http://localhost:8080
```

`proxy.conf.json` forwards `/api`, `/media`, and `/actuator` to the backend so the
SPA can be developed on http://localhost:4200.

```bash
npm run build      # production bundle
npm test           # Karma + Jasmine unit tests (headless Chrome)
```

---

## Configuration

All configuration is environment-variable driven; nothing sensitive is hardcoded.
See [`.env.example`](.env.example) for the full list. Key variables:

| Variable                    | Purpose                                              |
|-----------------------------|------------------------------------------------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection                     |
| `JWT_SECRET`                | Signing key (≥ 32 bytes) — **required in production** |
| `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL` | Token lifetimes (ISO-8601 durations)          |
| `CORS_ALLOWED_ORIGINS`      | Comma-separated browser origins                      |
| `STORAGE_PROVIDER`          | `local` (default) or `s3`                            |
| `STORAGE_LOCAL_ROOT`        | Media directory when `local`                         |
| `S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` | Object storage |
| `STORAGE_PUBLIC_BASE_URL`   | Public URL prefix for media in API responses         |
| `UPLOAD_MAX_BYTES`          | Max size per image (default 15 MB)                   |
| `RATE_LIMIT_CONTRIBUTE_CAPACITY`, `RATE_LIMIT_CONTRIBUTE_REFILL_MINUTES` | Contribution throttling |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD`, `MODERATOR_EMAIL`, `MODERATOR_PASSWORD` | Bootstrap accounts |
| `SEED_SAMPLE_DATA`          | Load sample records on startup                       |

---

## The public / no-login guarantee

The following are reachable with **no authentication**, by design:

- `GET /api/v1/icons/**`, `/saints/**`, `/churches/**`, `/map/**`,
  `/stats/**`, `/categories/**`, `/regions`
- `POST /api/v1/contributions` (anonymous contributions allowed)
- `GET /api/v1/contributions/status/{referenceCode}` (track a submission by code)
- `GET /media/**` (published images)

Everything under `/api/v1/admin/**` requires the `MODERATOR` or `ADMIN` role. The
authorization rules live in
[`SecurityConfig.java`](backend/src/main/java/et/eotc/archive/config/SecurityConfig.java).

---

## Contributing

Read [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md). In short: never commit secrets,
run the backend and frontend test suites before opening a pull request, and keep
public endpoints public.

## Licensing & ethics

Contributors confirm they hold the rights to the photographs they submit. Rights
holders may request correction, re-attribution, or removal of any record. Records
are written to avoid inventing names, dates, or theology; unknown values are shown
as unknown. See the in-app *Image Rights* and *Privacy* pages for the user-facing
policy.
