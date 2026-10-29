# Architecture

The application is an Angular SPA backed by a stateless Spring Boot API and a
Flyway-managed relational database. The `local` profile uses H2; `docker` and
`prod` use PostgreSQL. Media is stored on a local filesystem path and served
through `/media/**`.

```text
Angular SPA ──HTTP──▶ Spring Boot API ──JPA──▶ H2 or PostgreSQL
                           │
                           └──filesystem──▶ persistent media directory
```

## Backend packages

Backend source is under `com.eotcicon`:

- `config` — security rules, CORS, media resources, and API documentation
- `controller` — REST endpoints
- `domain` / `repository` — JPA model and database access
- `service` — authentication, contributions, moderation, and public queries
- `security` — JWT creation and request authentication
- `web.dto` / `web.mapper` — API payloads and mapping

Flyway owns the database schema. Hibernate runs in validation mode. PostgreSQL
migrations live in `src/main/resources/db/migration`; H2 mirrors live in
`src/main/resources/db/h2`. Keep versions aligned and do not edit an applied
migration.

## Contribution lifecycle

Anonymous or authenticated contributors submit records and photographs. New
records enter `PENDING`. Moderators can approve, reject, or request corrections;
each decision is recorded in `contribution_review`. Approval sets
`published_at`. Correction requests, rejection, pending submissions, and
archived records remain unavailable to public icon search and detail views.

Public icon queries filter on `PublicationStatus.APPROVED`. The public archive
statistic for icons counts only `APPROVED` records. Church and saint detail
pages build their depicted-icon lists from approved records as well.
The `/media/**` handler checks the owning icon's status too; unpublished media
is restricted to the contributor and moderators/admins.

## Access boundaries

Public routes support icon search/details, church and saint directories,
statistics, media retrieval, contribution submission, and reference-code status
lookups. Contributor routes are authenticated. `/api/v1/admin/submissions/**`
is available to moderators and admins; other `/api/v1/admin/**` management
routes are restricted to admins. Authentication uses bearer JWTs.

## Local and deployment configuration

See the root [README](../README.md) for the H2 and Docker Compose workflows,
environment variables, and deployment cautions. Compose is a local integration
stack, not a production deployment manifest. Production requires strong secrets,
TLS, persistent media storage, and database and media backups.
