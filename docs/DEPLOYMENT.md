# Deployment

## Docker Compose (local development / staging)

```bash
cp .env.example .env
# Optional — every value has a working default; edit the passwords to taste.
docker compose up --build -d
docker compose logs -f backend
```

Services and default host ports:

| Service   | Port(s)   | Notes                                             |
|-----------|-----------|---------------------------------------------------|
| frontend  | 8081 → 80 | Nginx serving the SPA; proxies `/api`, `/media` and `/actuator` to `backend` |
| backend   | 8080      | Spring Boot API (`SPRING_PROFILES_ACTIVE=docker` → PostgreSQL) |
| postgres  | 5432      | Flyway migrates on backend startup                |
| minio     | 9000, 9001| S3 API + web console; a bucket is created by `minio-init` |

The backend reaches storage through the in-network hostname `minio:9000`, while
browsers fetch images from `STORAGE_PUBLIC_BASE_URL` (default
`http://localhost:9000/eotc-media`). Change that to your public object-storage
domain in production.

The `frontend` waits for the `backend` health check, the `backend` waits for
`postgres` to be healthy and for `minio-init` to have created the bucket, so
the first request never hits an un-migrated database.

For the no-Docker workflow, see [`LOCAL_DEVELOPMENT.md`](LOCAL_DEVELOPMENT.md).

## Images

- **EOTC_ICON-backend/Dockerfile** — multi-stage: builds with `maven:3.9-eclipse-temurin-17`
  (`dependency:go-offline` layer first for caching), then runs the jar on
  `eclipse-temurin:17-jre` (`-XX:MaxRAMPercentage=75`). Runs as the non-root
  `appuser`. The container health check polls the public root endpoint.
- **EOTC_ICON-UI/Dockerfile** — `node:22-alpine` build (`npm ci` + production
  `ng build --base-href /`), served by `nginx:1.27-alpine` with history fallback
  and API proxying (`nginx.conf`).

Build individually:

```bash
docker build -t eotc-backend  ./EOTC_ICON-backend
docker build -t eotc-frontend ./EOTC_ICON-UI
```

## Configuration checklist

1. **Database** — set `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` to point at your
   managed Postgres. Do **not** run with `SPRING_PROFILES_ACTIVE=local`: that
   profile uses H2 and is intended for local development only. The `docker`
   and `prod` profiles fail fast when the three variables are missing.
2. **Storage** — `STORAGE_PROVIDER` and the `S3_*` variables are wired through
   the `docker` profile and the Compose file, but the storage abstraction is
   not implemented yet, so the backend does not read media from S3 today.
   `STORAGE_PUBLIC_BASE_URL` must point at the public media URL.
3. **Secrets** — `JWT_SECRET` (≥ 32 bytes, e.g. `openssl rand -base64 48`) and
   the `JWT_*` lifetimes are **not read yet**; they belong to the JWT auth
   feature that is still to be implemented. Never reuse the example values.
4. **CORS** — `CORS_ALLOWED_ORIGINS` is not read yet either. Until it exists,
   put the web app and the API behind the same origin (the Nginx image already
   proxies `/api` to the backend) so no CORS configuration is required.
5. **Bootstrap accounts** — `ADMIN_EMAIL`/`ADMIN_PASSWORD` (and the moderator
   equivalents) are reserved for the account-bootstrap feature; setting them
   has no effect yet.
6. **Docs exposure** — the OpenAPI/Swagger UI is not part of the backend yet;
   `springdoc.api-docs.enabled` and `springdoc.swagger-ui.enabled` only become
   meaningful once springdoc is added.
7. **Reverse proxy / TLS** — terminate TLS in front of the frontend container and
   forward `X-Forwarded-Proto`. The API is stateless and scales horizontally.

## Database migrations

Flyway runs automatically at startup and validates the schema. Never edit an applied
migration; add a new `V{n}__*.sql` under `EOTC_ICON-backend/src/main/resources/db/migration`.
That set targets PostgreSQL and is the only one used in production — the `local`
profile uses a separate hand-maintained H2 mirror under
`EOTC_ICON-backend/src/main/resources/db/h2`. Keep SQL portable and keep the two
sets version-aligned; PostgreSQL-only SQL lives solely in `db/migration`.

`V3__enable_trigram_search.sql` runs `CREATE EXTENSION pg_trgm`, so the role
that runs the migrations must be allowed to create extensions. The Compose
database user is the cluster superuser, so it works out of the box; on a
managed instance an administrator usually has to enable the extension once.

## Health & observability

- `GET /` — liveness: reports the application name and status.
- The container health checks poll that endpoint (backend) and the served
  `index.html` (frontend).
- Spring Boot Actuator (`/actuator/health`, `/actuator/info`) is not a
  dependency yet; add `spring-boot-starter-actuator` and
  `management.endpoints.web.exposure` when it is introduced.

## Backups

Back up PostgreSQL (e.g. `pg_dump`) and your media store (MinIO bucket or the local
media volume). The two together are the entire archive.
