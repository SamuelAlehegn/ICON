# Local development

Two supported ways to run the archive locally. Pick one — they share the same
code, the same Flyway migrations and the same API.

| | Path A — no Docker | Path B — Docker Compose |
|---|---|---|
| Database | H2 (in-memory or file) | PostgreSQL 16 in a container |
| Media storage | local filesystem | MinIO (S3-compatible) in a container |
| Profiles | `local` | `docker` |
| Extra tooling | JDK 17, Maven, Node 22 | Docker Desktop only |
| Use it for | writing code, fast tests, CI | integration work, realistic data, demoing |

The backend **never** silently falls back from PostgreSQL to H2: the `docker`
and `prod` profiles read their connection details from the environment and
fail fast with a readable message when one is missing.

---

## Path A — no Docker (H2)

The `local` profile is the **default**: with no `SPRING_PROFILES_ACTIVE` set,
the backend starts against an in-memory H2 database in PostgreSQL-compatibility
mode. No Docker, no PostgreSQL, no MinIO.

```bash
# Backend — http://localhost:8080, H2 console at /h2-console
cd EOTC_ICON-backend
mvn spring-boot:run

# Frontend — http://localhost:4200
cd EOTC_ICON-UI
npm install
npm start
```

`npm start` proxies `/api`, `/media` and `/actuator` to `http://localhost:8080`
(see `proxy.conf.json`), so the browser only ever talks to one origin and no
CORS configuration is needed.

### Keeping the data between restarts

The default H2 database lives in memory and is discarded on shutdown. Point
`H2_URL` at a file to make it persistent:

```bash
cd EOTC_ICON-backend
H2_URL='jdbc:h2:file:./data/eotcicon;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH' \
  mvn spring-boot:run
```

The database file is written to `EOTC_ICON-backend/data/` (git-ignored). Delete
that directory to start from scratch.

### Tests

```bash
cd EOTC_ICON-backend && mvn test        # H2, no external services
cd EOTC_ICON-UI    && npm test          # Karma + Jasmine, headless Chrome
```

The PostgreSQL integration test (`SchemaMigrationPostgresIT`) is skipped unless
`EOTC_PG_IT_URL` is set:

```bash
EOTC_PG_IT_URL=jdbc:postgresql://localhost:5432/eotcicon \
EOTC_PG_IT_USER=eotcicon EOTC_PG_IT_PASSWORD=eotcicon \
mvn verify -Dtest=SchemaMigrationPostgresIT
```

---

## Path B — Docker Compose

```bash
cp .env.example .env      # optional: every value has a working default
docker compose up --build
```

| Service | Host URL | Notes |
|---------|----------|-------|
| frontend | http://localhost:8081 | Nginx serving the Angular bundle; proxies `/api` and `/media` to the backend |
| backend | http://localhost:8080/api/v1 | Spring Boot API (`SPRING_PROFILES_ACTIVE=docker` → PostgreSQL) |
| postgres | localhost:5432 | Flyway migrates the schema on backend startup |
| minio | http://localhost:9000 | S3 API |
| minio console | http://localhost:9001 | MinIO web console |

The backend waits for PostgreSQL to pass its health check and for the media
bucket to be created, so the schema is migrated against a database that is
already accepting connections.

Useful commands:

```bash
docker compose logs -f backend       # follow the application log
docker compose ps                    # service state and health
docker compose down                  # stop, keep the data volumes
docker compose down -v               # stop and delete the data volumes
docker compose exec postgres psql -U eotcicon -d eotcicon
```

### What runs where

- **postgres** — PostgreSQL 16, data in the `postgres-data` volume.
  Flyway applies `db/migration` (the PostgreSQL migration set) at startup.
- **minio** — S3-compatible object storage, data in the `minio-data` volume.
- **minio-init** — one-shot job that creates the media bucket and makes it
  publicly readable. It runs before the backend starts.
- **backend** — multi-stage image (Maven build → Temurin 17 JRE), runs as a
  non-root user with `SPRING_PROFILES_ACTIVE=docker`.
- **frontend** — Node 22 build → Nginx serving `dist/eotc-icon-ui/browser`
  with an SPA history fallback.

---

## Spring profiles

| Profile | Database | Migration set | Activated by |
|---------|----------|---------------|--------------|
| `local` *(default)* | H2 — in-memory, or file via `H2_URL` | `classpath:db/h2` | nothing (it is the default) |
| `docker` | PostgreSQL — `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `classpath:db/migration` | `SPRING_PROFILES_ACTIVE=docker` |
| `prod` | PostgreSQL — same variables, no defaults | `classpath:db/migration` | `SPRING_PROFILES_ACTIVE=prod` |

The `docker` and `prod` profiles are identical except that `docker` is meant
for the Compose stack and `prod` for a real deployment. Both refuse to start
without `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`
([`PostgresDatabaseConfig.java`](../EOTC_ICON-backend/src/main/java/com/eotcicon/config/PostgresDatabaseConfig.java)).

Running against a PostgreSQL that is not in Compose:

```bash
cd EOTC_ICON-backend
SPRING_PROFILES_ACTIVE=docker \
DB_URL=jdbc:postgresql://localhost:5432/eotcicon \
DB_USERNAME=eotcicon DB_PASSWORD=eotcicon \
mvn spring-boot:run
```

---

## Environment variables

### Read by the backend today

| Variable | Used by | Required | Default | Purpose |
|----------|---------|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | all | no | `local` | `local`, `docker` or `prod` |
| `DB_URL` | `docker`, `prod` | **yes** | — | PostgreSQL JDBC URL |
| `DB_USERNAME` | `docker`, `prod` | **yes** | — | PostgreSQL user |
| `DB_PASSWORD` | `docker`, `prod` | **yes** | — | PostgreSQL password |
| `SERVER_PORT` | all | no | `8080` | HTTP port of the backend |
| `H2_URL` | `local` | no | in-memory H2 | JDBC URL; set it to a `jdbc:h2:file:…` URL for a persistent database |
| `STORAGE_PROVIDER` | `docker` | no | `s3` | `local` or `s3` |
| `STORAGE_LOCAL_ROOT` | `docker` | no | `./data/media` | Media directory for the `local` provider |
| `STORAGE_PUBLIC_BASE_URL` | `docker` | no | `http://localhost:9000/eotc-media` | Public media URL prefix |
| `S3_ENDPOINT` | `docker` | no | `http://minio:9000` | S3 endpoint (MinIO inside the Compose network) |
| `S3_BUCKET` | `docker` | no | `eotc-media` | Bucket that holds the media |
| `S3_ACCESS_KEY` | `docker` | no | `minioadmin` | S3 access key |
| `S3_SECRET_KEY` | `docker` | no | `minioadmin` | S3 secret key |

The `STORAGE_*` / `S3_*` variables are declared by the `docker` profile and
are wired into the container by Compose, but the storage abstraction itself is
not implemented yet — the backend does not read media from S3 at the moment.

### Used by Docker Compose

| Variable | Default | Purpose |
|----------|---------|---------|
| `FRONTEND_PORT` | `8081` | Host port of the web app |
| `BACKEND_PORT` | `8080` | Host port of the API |
| `POSTGRES_PORT` | `5432` | Host port of PostgreSQL |
| `MINIO_API_PORT` | `9000` | Host port of the MinIO S3 API |
| `MINIO_CONSOLE_PORT` | `9001` | Host port of the MinIO console |
| `DB_NAME` | `eotcicon` | Database name — also used to build `DB_URL` for the backend |
| `DB_USERNAME` / `DB_PASSWORD` | `eotcicon` | PostgreSQL credentials, shared by the container and the backend |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` | `minioadmin` | MinIO root credentials |
| `MINIO_BUCKET` | `eotc-media` | Bucket created by `minio-init` |

### Read by the test suite

| Variable | Used by | Default | Purpose |
|----------|---------|---------|---------|
| `EOTC_PG_IT_URL` | `SchemaMigrationPostgresIT` | — | Enables the PostgreSQL integration test; the test is skipped when unset |
| `EOTC_PG_IT_USER` | `SchemaMigrationPostgresIT` | `postgres` | User for that database |
| `EOTC_PG_IT_PASSWORD` | `SchemaMigrationPostgresIT` | `postgres` | Password for that database |

### Reserved — not read by the backend yet

These belong to features that are still to be implemented. They are listed so
the target configuration surface is known; setting them has no effect until
the matching backend code lands.

`JWT_SECRET`, `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`, `CORS_ALLOWED_ORIGINS`,
`UPLOAD_MAX_BYTES`, `RATE_LIMIT_CONTRIBUTE_CAPACITY`,
`RATE_LIMIT_CONTRIBUTE_REFILL_MINUTES`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`,
`MODERATOR_EMAIL`, `MODERATOR_PASSWORD`, `SEED_SAMPLE_DATA`.

---

## Troubleshooting

**`Missing required environment variable 'DB_URL'`** — a PostgreSQL profile
was activated without connection details. Either drop `SPRING_PROFILES_ACTIVE`
to fall back to H2, or set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

**Backend restarts waiting for the database** — PostgreSQL is not healthy yet.
`docker compose logs postgres` shows the reason; the usual cause is a
`postgres-data` volume created with different credentials. Reset it with
`docker compose down -v`.

**Frontend loads but every API call 502s** — the backend is not up. Check
`docker compose logs backend`; the frontend waits for the backend health check
before it starts, so this usually means the backend crashed during migration.

**Port already in use** — override the host port in `.env`
(`FRONTEND_PORT=8082`, `BACKEND_PORT=8083`, …).

**Flyway checksum mismatch** — a migration that was already applied was edited.
Restore the file and add a new `V{n}__*.sql` instead. See
[`docs/DATABASE.md`](DATABASE.md).
