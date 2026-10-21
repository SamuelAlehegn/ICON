-- =============================================================================
-- V3__enable_trigram_search.sql — pg_trgm extension and GIN trigram indexes
-- Migration set: classpath:db/migration (profile `prod`, the default config)
-- =============================================================================
--
-- Conventions (see README.md, "Database migrations"):
--   * Flyway owns the schema; Hibernate runs with ddl-auto=validate and never
--     creates or alters tables.
--   * Keep SQL portable (ANSI) wherever possible so the H2 mirror (db/h2) can
--     carry the same migration at the same version number.
--   * PostgreSQL-specific SQL (pg_trgm, GIN indexes, native types, functions)
--     belongs ONLY in this set. Always add a same-version placeholder file to
--     db/h2 that documents why the migration does not apply to H2.
--   * Never edit an applied migration — checksums are validated at startup.
--     Add a new V{n}__*.sql instead.
--
-- Fuzzy search support: the public `q` parameter matches title / canonical
-- name / church name with ILIKE '%term%'. Plain B-tree indexes cannot serve
-- that pattern, so the three searched columns get a GIN index over trigrams.
--
-- The H2 mirror of this file lives at db/h2/V3__enable_trigram_search.sql and
-- is intentionally a no-op placeholder.
--
-- NOTE: pg_trgm is not a "trusted" extension — the role that runs the
-- migration must be allowed to CREATE EXTENSION, or an administrator has to
-- run "CREATE EXTENSION pg_trgm;" once before the application starts.
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_church_name_trgm ON church USING gin (name gin_trgm_ops);
CREATE INDEX idx_saint_canonical_name_trgm ON saint USING gin (canonical_name gin_trgm_ops);
CREATE INDEX idx_icon_title_trgm ON icon USING gin (title gin_trgm_ops);
