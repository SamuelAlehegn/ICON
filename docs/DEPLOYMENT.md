# Deployment

## Docker Compose (recommended for staging / self-hosting)

```bash
cp .env.example .env
# Edit .env — at minimum set JWT_SECRET and the database/MinIO/account passwords.
docker compose up --build -d
docker compose logs -f backend
```

Services and default host ports:

| Service   | Port(s)   | Notes                                             |
|-----------|-----------|---------------------------------------------------|
| frontend  | 8081 → 80 | Nginx serving the SPA; proxies `/api` and `/media` to `backend` |
| backend   | 8080      | Spring Boot API; `/actuator/health` for probes    |
| postgres  | 5432      | Flyway migrates on backend startup                |
| minio     | 9000, 9001| S3 API + web console; a bucket is created by `minio-init` |

The backend reaches storage through the in-network hostname `minio:9000`, while
browsers fetch images from `STORAGE_PUBLIC_BASE_URL` (default
`http://localhost:9000/eotc-media`). Change that to your public object-storage
domain in production.

## Images

- **backend/Dockerfile** — multi-stage: builds with `maven:3.9-eclipse-temurin-17`
  (`dependency:go-offline` layer first for caching), then runs the jar on
  `eclipse-temurin:17-jre` (`-XX:MaxRAMPercentage=75`). Runs as the non-root
  `appuser`. Includes an `actuator/health` healthcheck.
- **frontend/Dockerfile** — `node:22-alpine` build (`npm ci` + production
  `ng build`), served by `nginx:1.27-alpine` with history fallback and API proxying.

Build individually:

```bash
docker build -t eotc-backend  ./backend
docker build -t eotc-frontend ./frontend
```

## Configuration checklist

1. **Secrets** — set a strong `JWT_SECRET` (≥ 32 bytes, e.g. `openssl rand -base64 48`).
   Never reuse the example value.
2. **Database** — point `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` at your managed Postgres.
   Do **not** run with `SPRING_PROFILES_ACTIVE=local`: that profile uses an in-memory
   H2 database intended for local development only.
3. **Storage** — set `STORAGE_PROVIDER=s3` and the `S3_*` variables for object
   storage, or `local` with a mounted persistent volume for a single-node deploy.
   Set `STORAGE_PUBLIC_BASE_URL` to the public media URL.
4. **CORS** — set `CORS_ALLOWED_ORIGINS` to your real web origin(s).
5. **Bootstrap accounts** — set `ADMIN_EMAIL`/`ADMIN_PASSWORD` (and moderator
   equivalents) for the first boot, then rotate or remove the variables.
6. **Docs exposure** — optionally disable `springdoc.api-docs.enabled` and
   `springdoc.swagger-ui.enabled` in production.
7. **Reverse proxy / TLS** — terminate TLS in front of the frontend container and
   forward `X-Forwarded-Proto`. The API is stateless and scales horizontally.

## Database migrations

Flyway runs automatically at startup and validates the schema. Never edit an applied
migration; add a new `V{n}__*.sql` under `backend/src/main/resources/db/migration`.

## Health & observability

- `GET /actuator/health` — readiness/liveness (public).
- `GET /actuator/info` — build info.
- Other actuator endpoints are not exposed (see `management.endpoints.web.exposure`).

## Backups

Back up PostgreSQL (e.g. `pg_dump`) and your media store (MinIO bucket or the local
media volume). The two together are the entire archive.
