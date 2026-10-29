# Deployment notes

The checked-in `docker-compose.yml` is a local integration/demo stack. It binds
host ports to loopback and includes development credential fallbacks; do not
expose it as a production deployment without replacing those settings and
adding an operational environment appropriate to the deployment.

## Production profile requirements

Run the backend with `SPRING_PROFILES_ACTIVE=prod` and supply:

- `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` for PostgreSQL
- `JWT_SECRET` containing at least 32 UTF-8 bytes
- `CORS_ALLOWED_ORIGINS` as a comma-separated list of the exact web origins
- `STORAGE_LOCAL_ROOT` pointing to durable storage mounted read/write by the
  non-root backend process
- optionally `JWT_ACCESS_TOKEN_EXPIRATION_MS` (default `900000`) and
  `SERVER_PORT` (default `8080`)

Never use the example `.env` values in production. Supply credentials through a
secret manager, terminate TLS at a trusted reverse proxy, and configure the
proxy to forward the original host and protocol. Back up the PostgreSQL database
and the media directory together.

## Container images

- `EOTC_ICON-backend/Dockerfile` builds with Maven and runs the Spring Boot jar
  on Java 17 as a non-root user. The Compose stack mounts the `media-data`
  volume at `/var/lib/eotcicon/media`.
- `EOTC_ICON-UI/Dockerfile` builds Angular with Node 22 and serves it with Nginx.
  Nginx proxies `/api` and `/media` to the backend.

For local startup, test commands, host ports, and the full variable reference,
see the root [README](../README.md).

## Database migrations

Flyway applies PostgreSQL migrations on startup. The `docker` and `prod` profiles
fail without PostgreSQL connection variables; they do not fall back to H2. H2
uses the parallel migration set for local development and automated tests. Keep
the migration sequences aligned and add a new version rather than editing an
applied migration.
