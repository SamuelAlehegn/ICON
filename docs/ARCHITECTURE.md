# Architecture

## Overview

The system is a classic three-tier application with an object-storage sidecar for
images:

```
┌────────────────────┐        ┌──────────────────────────┐        ┌──────────────┐
│  Angular 19.2 SPA  │  HTTP  │  Spring Boot 4.0 API     │  JPA   │  PostgreSQL  │
│  (standalone,      │ ─────▶ │  (stateless, JWT auth)   │ ─────▶ │  16          │
│   signals,         │        │                          │        └──────────────┘
│   Material, SCSS)  │        │  Flyway-managed schema   │
└────────────────────┘        │                          │        ┌──────────────┐
                              │  StorageService ─────────┼──────▶ │ Local FS or  │
                              └──────────────────────────┘        │ S3 / MinIO   │
                                                                  └──────────────┘
```

## Backend

Package root: `et.eotc.archive`.

| Package      | Responsibility                                                        |
|--------------|-----------------------------------------------------------------------|
| `config`     | `SecurityConfig` (auth rules), `WebConfig` (media resource handler), `AppProperties` (typed config), `OpenApiConfig` |
| `domain`     | JPA entities and enums                                                |
| `repository` | Spring Data JPA repositories + `Specification` builders              |
| `service`    | Business logic (query, contribution, moderation, auth, storage, imaging) |
| `web`        | REST controllers and DTOs                                             |
| `security`   | JWT filter, entry point, `AuthenticatedUser`, user details service    |
| `bootstrap`  | `AdminBootstrap` and `SampleDataSeeder`                               |

### Data model

Core tables (see [`DATABASE.md`](DATABASE.md) and
`db/migration/V2__create_core_schema.sql`):

- `church` — physical location of artworks; supports dedup via `merged_into_id`.
- `church_alias` — alternative names of a church.
- `saint`   — depicted figures; unique `slug`; dedup via `merged_into_id`.
- `saint_alias` — alternative names of a saint.
- `icon`    — the artwork record; unique `reference_code`; publication status.
- `icon_image` — one or more photographs per icon.
- `icon_subject` — many-to-many between icons and saints, with the icon's own
  caption and a `figure_certainty`. `saint_id` is nullable, so an icon can
  depict an unidentified figure.
- `image_rights` — licence and attribution statement of an image.
- `contribution_review` — immutable moderation trail.
- `audit_log` — administrative action log.
- `users`, `refresh_token` — authentication.

Every table includes `created_at`/`updated_at` (from `BaseEntity`). Deduplication is
first-class: churches and saints can be merged, with self-referencing
`merged_into_id` foreign keys. Trigram (`pg_trgm`) GIN indexes back fuzzy text
search.

The schema is owned exclusively by Flyway (`spring.jpa.hibernate.ddl-auto=validate`),
so entity/schema drift fails fast at startup.

#### Configuration profiles

| Profile | Database | Migrations | Use |
|---------|----------|------------|-----|
| `local` *(default)* | H2 — in-memory, or file via `H2_URL` | `classpath:db/h2` | local development and tests, no Docker |
| `docker` | PostgreSQL (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) | `classpath:db/migration` | Docker Compose stack |
| `prod` | PostgreSQL (`DB_URL`/`DB_USERNAME`/`DB_PASSWORD`) | `classpath:db/migration` | production |
| `test`  | H2 (via `local`) | `classpath:db/h2` | automated tests |

The `local` profile keeps Flyway ownership and `ddl-auto: validate`, but uses a
hand-maintained H2 mirror of the PostgreSQL schema because H2 has no `pg_trgm`
extension or GIN indexes. It lives in `db/h2` (not under `db/migration`, which
Flyway scans recursively). Keep the two migration sets in sync when the schema
changes; production never uses H2.

The `docker` and `prod` profiles are identical and both read their connection
details from the environment, so neither can silently fall back to H2:
`PostgresDatabaseConfig` fails startup with a readable message when
`DB_URL`, `DB_USERNAME` or `DB_PASSWORD` is missing.

### Public vs. privileged access

Browsing the published archive is deliberately public. `SecurityConfig` permits all
`GET`s for icons, saints, churches, map, stats, categories, and regions, anonymous
`POST /api/v1/contributions`, and status lookups by reference code. Only
`/api/v1/admin/**` requires `MODERATOR`/`ADMIN`. The API is stateless; authentication
uses short-lived JWT access tokens plus rotating refresh tokens.

Private data (contributor contact email) is never serialized into public DTOs and is
covered by an integration test.

### Image pipeline

Submitted images pass through `ImageValidator` (magic-byte content sniffing, size
and type allow-list) and `ThumbnailGenerator` (ImageIO — no external native deps).
`StorageService` has two implementations selected by `app.storage.provider`:

- `LocalStorageService` — writes to `app.storage.local-root`, served by `WebConfig`
  at `/media/**`.
- `S3StorageService` — writes to any S3-compatible endpoint (MinIO included).

Storage keys are `icons/{iconId}/{uuid}.{ext}`, so a rejected submission's images can
be cleaned up in isolation. `MediaUrlResolver` builds public URLs from
`app.storage.public-base-url`.

### Content lifecycle

`POST /api/v1/contributions` → status `PENDING` → moderator decision:

- `APPROVE` → icon and its (new or matched) church/saint become `APPROVED`, with
  `published_at` set.
- `REQUEST_CORRECTION` → contributor is asked to fix details; stays non-public.
- `REJECT` → not published; images cleaned up.

Only `APPROVED` records are returned by the public query services.

## Frontend

Angular 19.2, standalone components, signals, and the new `@if`/`@for` control flow.
Key directories:

- `core/api` — one typed service per backend resource.
- `core/auth` — `AuthService`, token storage, and an HTTP interceptor that
  transparently refreshes the access token once on 401.
- `core/guards` — `authGuard`, `moderatorGuard`.
- `shared` — `icon-card`, `state` (loading/empty/error).
- `features` — lazy-loaded pages: home, gallery, icon/church/saint detail, map
  (Leaflet), contribute, submission status, login, account, admin, and static pages.

Design system: ivory/parchment/burgundy/gold tokens defined in `styles.scss` with an
Angular Material M3 theme; typography combines Cormorant Garamond, Source Sans 3, and
Noto Serif Ethiopic.

## Deployment

See [`DEPLOYMENT.md`](DEPLOYMENT.md) and [`LOCAL_DEVELOPMENT.md`](LOCAL_DEVELOPMENT.md).
In short: a multi-stage backend image
(build on Temurin 17, run on JRE 17 as a non-root user) and an Nginx frontend image
that serves the SPA with history fallback and proxies `/api` and `/media` to the
backend. `docker-compose.yml` wires PostgreSQL, MinIO, the API, and the web app
together for a one-command local/production-like run.
