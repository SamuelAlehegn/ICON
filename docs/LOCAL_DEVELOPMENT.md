# Local development

The supported H2 and Docker Compose workflows, ports, and environment-variable
reference are maintained in the repository [README](../README.md). The H2 path
requires no Docker. The Compose path uses PostgreSQL and persistent local media
storage.

## Tests

Backend tests use the `local` Spring profile and H2 by default:

```bash
cd EOTC_ICON-backend
./mvnw test
```

The optional PostgreSQL migration tests run only when `EOTC_PG_IT_URL` is set.
