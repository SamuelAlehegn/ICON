# EOTC Sacred Icons Archive

A searchable archive for documenting and preserving Ethiopian Orthodox Tewahedo
Church icons and religious wall paintings. Public browsing requires no account;
contributors can submit images for review, and moderators decide whether a
submission becomes public.

## Run locally with H2 (no Docker)

Requirements: Java 17 and Node.js 22 (for the Angular frontend). The backend's
default `local` profile uses an in-memory H2 database in PostgreSQL compatibility
mode and applies the H2 Flyway migrations at startup.

Start the backend from one terminal:

```bash
cd EOTC_ICON-backend
./mvnw spring-boot:run
```

The API is at <http://localhost:8080>. H2 console is at
<http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:eotcicon`, user
`eotcicon`, blank password). Data is discarded when the process stops.

Start the frontend from another terminal:

```bash
cd EOTC_ICON-UI
npm ci
npm start
```

Open <http://localhost:4200>. The Angular development proxy forwards API and
media requests to the backend. The backend serves uploaded files from
`./data/media` by default.

To persist H2 data between restarts, set `H2_URL` to a file URL:

```bash
cd EOTC_ICON-backend
H2_URL='jdbc:h2:file:./data/eotcicon;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH' \
  ./mvnw spring-boot:run
```

Run backend tests locally with H2 and no Docker:

```bash
cd EOTC_ICON-backend
./mvnw test
```

## Run with Docker Compose and PostgreSQL

Docker Compose is intended for local integration and demo use. It runs the SPA,
Spring API, PostgreSQL 16, and persistent filesystem media storage. The Docker
profile uses PostgreSQL and the PostgreSQL Flyway migrations; it never falls
back to H2.

```bash
cp .env.example .env
# Keep the example values for local development, or change them for your own setup.
docker compose up --build
```

| Service | Address | Notes |
| --- | --- | --- |
| Website | <http://localhost:8081> | Nginx serves Angular and proxies `/api` and `/media` |
| API | <http://localhost:8080> | Spring Boot REST API |
| PostgreSQL | `localhost:5432` | Persistent `postgres-data` volume |

The backend starts after PostgreSQL reports healthy. Uploaded media is stored in
the persistent `media-data` volume. Useful commands:

```bash
docker compose ps
docker compose logs -f backend
docker compose down       # keep the database and media volumes
docker compose down -v    # delete the database and media volumes
```

The Compose file publishes ports on loopback and uses development credentials.
Do not use its defaults as production credentials or treat this file as a
production deployment manifest. A deployment needs managed secrets, TLS, a
backed-up persistent media volume, database backups, and an appropriately
configured reverse proxy.

## Environment configuration

Copy `.env.example` for local Compose overrides. Do not commit `.env`. The
variables that the application or Compose currently reads are:

| Variable | Used by | Default / requirement | Purpose |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Backend | `local` when unset | `local` selects H2; `docker` and `prod` select PostgreSQL |
| `H2_URL` | Backend, `local` | In-memory H2 | Optional H2 JDBC URL; use a file URL to persist local data |
| `DB_URL` | Backend, `docker` / `prod` | Required | PostgreSQL JDBC URL |
| `DB_USERNAME` | Backend, `docker` / `prod` | Required | PostgreSQL account |
| `DB_PASSWORD` | Backend, `docker` / `prod` | Required | PostgreSQL password |
| `DB_NAME` | Compose | `eotcicon` | Database name used by the PostgreSQL container and backend URL |
| `JWT_SECRET` | Backend, `docker` / `prod` | Required there; at least 32 UTF-8 bytes | JWT signing key. Local profile has a development-only fallback |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Backend | `900000` | Access-token lifetime in milliseconds |
| `CORS_ALLOWED_ORIGINS` | Backend, `docker` / `prod` | Required there | Comma-separated allowed origins; the Compose example allows local development origins |
| `STORAGE_LOCAL_ROOT` | Backend | `./data/media` locally; required by `prod` | Directory for uploaded media; must be persistent in deployment |
| `SERVER_PORT` | Backend | `8080` | Backend listen port |
| `FRONTEND_PORT` | Compose | `8081` | Host port for the website |
| `BACKEND_PORT` | Compose | `8080` | Host port for direct API access |
| `POSTGRES_PORT` | Compose | `5432` | Host port for PostgreSQL |
| `EOTC_PG_IT_URL` | Tests | Unset | Enables optional PostgreSQL migration integration tests |
| `EOTC_PG_IT_USER` | Tests | `postgres` | PostgreSQL integration-test user |
| `EOTC_PG_IT_PASSWORD` | Tests | `postgres` | PostgreSQL integration-test password |

`docker` and `prod` fail during startup when required database details are
missing. The `prod` profile also requires `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`,
and `STORAGE_LOCAL_ROOT`. Generate a secret outside source control and supply it
through the deployment secret manager. The Compose `.env` values are for local
development only.

## Contribution guidelines

- Branch from `main` and keep changes focused.
- Never commit credentials, tokens, `.env`, generated output, or uploaded media.
- Add backend schema changes as new Flyway migrations; do not edit migrations
  that have already been applied. Keep the PostgreSQL and H2 migration sets in
  sync, with a documented H2 placeholder when a migration is PostgreSQL-only.
- Add or update tests for changed behavior. Run `./mvnw test` in the backend and
  the relevant frontend checks before opening a pull request.
- Keep public DTOs free of private contributor contact details and moderation
  notes. Public endpoints must expose only approved icon records.
- Describe behavior changes, configuration changes, and any operational impact
  in the pull request.

## Image rights, attribution, and review

Submit only photographs you took or images for which you have permission to
submit and display them. A submission does not transfer ownership of the image
to the archive. Contributors are responsible for identifying applicable
copyright, privacy, and site-photography restrictions before uploading.

Contributors can request public credit by display name or choose anonymous
attribution. Contact email is collected for archive communication and is kept
out of public icon responses. Moderators review submissions before publication;
pending, rejected, correction-requested, and archived records are not part of
the public icon gallery or published-icon count. Rights holders may request
correction, attribution changes, or removal through the archive maintainers.
Direct media URLs follow the same publication boundary: unpublished images are
available only to the submission owner and moderators/admins.

Do not guess or invent an artist, subject, date, location, license, or religious
interpretation. Mark uncertain information as unknown and include only context
that can be supported by the contributor or a reliable source.

## Public visibility guarantee

Public icon search and detail lookup are constrained to `APPROVED` records in
`IconService`. Church and saint detail pages also query approved depictions, and
`StatisticsService` counts published icons using `countByPublicationStatus(APPROVED)`.
The H2 integration test
`ContributionModerationPublicJourneyIntegrationTest` exercises search, detail,
submission, moderator review, publication, and verifies that unapproved and
rejected records remain hidden from public results and published-icon counts.

## Project documentation

- [`docs/API.md`](docs/API.md) — endpoint reference
- [`docs/DATABASE.md`](docs/DATABASE.md) — schema and migration conventions
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — design overview
- [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) — contribution details
