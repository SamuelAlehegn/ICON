# API contract

Base URL: `/api/v1`. Interactive docs are served at `/swagger-ui.html`
(OpenAPI JSON at `/api-docs`).

- All responses are JSON (`application/json`) unless noted.
- Timestamps are ISO-8601 in UTC.
- IDs are UUIDs unless stated otherwise.
- Errors use Spring's problem-style body with `status`, `error`, and `message`
  (messages are not stack traces — `server.error.include-stacktrace=never`).

This document has two parts: **the contract** (the full endpoint matrix,
grouped by resource) and **the details** (payloads, parameters and examples
for the endpoints that need them).

## Access model

The archive is public — browsing needs no account. Only the endpoints marked
*Contributor*, *Moderator* or *Admin* require a token.

| Level | Meaning |
|-------|---------|
| **Public** | No credentials. Read-only, except for the anonymous contribution endpoint. |
| **Public · rate limited** | No credentials, but throttled per client IP; over the limit returns `429`. |
| **Public · token in body** | No access token; the request body must carry a valid refresh token. |
| **Contributor** | Any authenticated account (`CONTRIBUTOR`, `MODERATOR` or `ADMIN`). |
| **Moderator** | `MODERATOR` or `ADMIN` role. |
| **Admin** | `ADMIN` role only. |

| Situation | Result |
|-----------|--------|
| No / invalid / expired token on a protected endpoint | `401 Unauthorized` |
| Valid token, insufficient role | `403 Forbidden` |
| Unknown id or slug | `404 Not Found` |
| Unpublished record requested publicly | `404 Not Found` (never `403` — existence is not leaked) |
| Validation failure | `400 Bad Request` with per-field errors |
| Rate limit exceeded | `429 Too Many Requests` |

Protected requests carry `Authorization: Bearer <accessToken>`. Access tokens
are short-lived (15 min); refresh tokens are rotated on use and revoked on
logout.

Every paginated endpoint returns the same envelope:

```json
{
  "content": [],
  "page": 0, "size": 12, "totalElements": 0, "totalPages": 0,
  "first": true, "last": true
}
```

`page` is zero-based, `size` defaults to `12`.

---

# The contract

## Icons

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/icons` | Search published icons. Filters: `q`, `region`, `churchId`, `saintId`, `category`; `sort`, `page`, `size`. | Public |
| GET | `/api/v1/icons/{idOrSlug}` | Full icon detail: subjects, images, location, dates, historical notes, related icons. | Public |

Only `APPROVED` icons are returned; anything else is a `404`.

## Saints

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/saints` | Directory search. Filters: `q`, `category`; `sort`, `page`, `size`. | Public |
| GET | `/api/v1/saints/{idOrSlug}` | Saint detail: canonical, Amharic and Ge'ez names, alternative names, depictions, churches. | Public |

## Churches

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/churches` | Directory search. Filters: `q`, `region`; `sort`, `page`, `size`. | Public |
| GET | `/api/v1/churches/{idOrSlug}` | Church detail: names, location, coordinates, description, its published icons. | Public |
| GET | `/api/v1/churches/map/markers` | Churches that have coordinates, with icon counts, for the map. | Public |

## Archive metadata

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/stats` | Counts: published icons, churches, saints, distinct regions, contributors, sample-data flag. | Public |
| GET | `/api/v1/regions` | Distinct regions in the published archive. | Public |
| GET | `/api/v1/categories` | Distinct icon categories. | Public |

## Contributions

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| POST | `/api/v1/contributions` | Submit an icon: multipart metadata plus one or more images. | Public · rate limited |
| GET | `/api/v1/contributions/status/{referenceCode}` | Status of a submission by its reference code (`EOTC-XXXXXXXXXXXX`), with moderator notes. | Public |

## Auth

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| POST | `/api/v1/auth/login` | Exchange email + password for an access/refresh token pair. | Public |
| POST | `/api/v1/auth/refresh` | Rotate a refresh token and issue a new pair. | Public · token in body |
| POST | `/api/v1/auth/logout` | Revoke the supplied refresh token. | Public · token in body |
| GET | `/api/v1/auth/me` | Profile and role of the authenticated caller. | Contributor |
| GET | `/api/v1/auth/health` | Liveness probe for the auth subsystem. | Public |

## Account

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/account/submissions` | The caller's own submissions with status and review history. | Contributor |
| GET | `/api/v1/account/submissions/{id}` | One of the caller's own submissions. | Contributor |
| PATCH | `/api/v1/account/submissions/{id}` | Update permitted fields on an own pending submission. `PUT` is accepted too. | Contributor |
| POST | `/api/v1/account/submissions/{id}/request-correction` | Ask the archive team to correct the submission (sets `NEEDS_CORRECTION`). | Contributor |
| POST | `/api/v1/account/submissions/{id}/request-removal` | Ask for the submission to be removed (archives it and records a `WITHDRAW` review). | Contributor |

## Admin

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/admin/submissions` | Moderation queue. Filter: `status` (default `PENDING`); `page`, `size`. | Moderator |
| GET | `/api/v1/admin/submissions/{id}` | Submission detail including the private contact email and the review history. | Moderator |
| POST | `/api/v1/admin/submissions/{id}/decision` | Apply a decision (`APPROVE`, `REJECT`, `REQUEST_CORRECTION`) with optional notes. | Moderator |

### Proposed, not yet in the project spec

Listed here so the surface is known; implement only when the matching backend
feature exists.

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/api/v1/admin/audit-log` | Browse the audit trail (who, what, which entity, from which IP). | Admin |
| POST | `/api/v1/admin/churches/{id}/merge` | Merge a duplicate church into its canonical record. | Admin |
| POST | `/api/v1/admin/saints/{id}/merge` | Merge a duplicate saint into its canonical record. | Admin |

## Outside the versioned API

| Method | Path | Description | Access |
|--------|------|-------------|--------|
| GET | `/media/**` | Stored images and thumbnails (local storage provider only). | Public |
| GET | `/swagger-ui.html`, `/api-docs` | Interactive and raw OpenAPI documentation. | Public |
| GET | `/actuator/health`, `/actuator/info` | Health and build information. | Public |

---

# Details

## Icons

`GET /api/v1/icons` — search published icons.

| Query        | Type   | Default  | Notes                                   |
|--------------|--------|----------|-----------------------------------------|
| `q`          | string | —        | Fuzzy text search (title, figure, church) |
| `region`     | string | —        | Filter by region                        |
| `churchId`   | uuid   | —        | Filter by church                        |
| `saintId`    | uuid   | —        | Filter by depicted saint                |
| `category`   | string | —        | Filter by category                      |
| `sort`       | enum   | `newest` | `newest`, `oldest`, `title`             |
| `page`       | int    | `0`      | Zero-based page                         |
| `size`       | int    | `12`     | Page size                               |

Returns a `PageResponse<IconSummaryDto>`:

```json
{
  "content": [
    {
      "id": "…", "slug": "saint-george-on-horseback",
      "title": "Saint George on horseback",
      "depictedFigure": "Saint George", "figureCertainty": "IDENTIFIED",
      "saintNames": ["Saint George"],
      "churchName": "Bete Giyorgis", "churchSlug": "bete-giyorgis",
      "town": "Lalibela", "region": "Amhara", "country": "Ethiopia",
      "contributorCredit": "Anonymous contributor",
      "thumbnailUrl": "http://…/media/icons/…/thumb.jpg",
      "imageUrl": "http://…/media/icons/…/full.jpg",
      "altText": "Saint George at Bete Giyorgis"
    }
  ],
  "page": 0, "size": 12, "totalElements": 1, "totalPages": 1,
  "first": true, "last": true
}
```

`GET /api/v1/icons/{idOrSlug}` — full `IconDetailDto` (subjects, images, location
and coordinates, artwork date, historical notes, related icons by saint, more from
the same church). Only `APPROVED` icons are returned; anything else yields `404`.

## Churches

| Method | Path                        | Description                                  |
|--------|-----------------------------|----------------------------------------------|
| GET    | `/churches`                 | Directory search (`q`, `region`, `sort`, `page`, `size`) |
| GET    | `/churches/{idOrSlug}`      | Church detail with its published icons       |
| GET    | `/churches/map/markers`     | Churches with usable coordinates for the map |

## Saints

| Method | Path                    | Description                                             |
|--------|-------------------------|---------------------------------------------------------|
| GET    | `/saints`               | Directory search (`q`, `category`, `sort`, `page`, `size`) |
| GET    | `/saints/{idOrSlug}`    | Saint detail with depictions and churches               |

## Archive metadata

| Method | Path                      | Description                                    |
|--------|---------------------------|------------------------------------------------|
| GET    | `/stats`                  | Published counts (icons, churches, saints, regions) |
| GET    | `/regions`                | Distinct regions in the published archive      |
| GET    | `/categories`             | Distinct icon categories                       |

## Contributions

`POST /api/v1/contributions` — `multipart/form-data`.

| Part                     | Required | Notes                                        |
|--------------------------|----------|----------------------------------------------|
| `images`                 | yes      | Repeatable; JPEG/PNG/WebP/TIFF, ≤ 15 MB each |
| `churchName`             | yes      | Church or monastery name                     |
| `town`, `region`, `country` | no    | Location details                             |
| `latitude`, `longitude`  | no       | Coordinates                                  |
| `saintName`              | no       | Depicted figure, if known                    |
| `category`               | no       | e.g. icon, mural, wall painting              |
| `title`, `description`   | no       | Descriptive text                             |
| `historicalNotes`        | no       |                                              |
| `capturedDate`           | no       |                                              |
| `attributionPreference`  | no       | `DISPLAY_NAME` or `ANONYMOUS`                |
| `contributorDisplayName` | no       | Used only when attribution is `DISPLAY_NAME` |
| `contactEmail`           | no       | **Private** — never exposed publicly         |
| `consent`                | yes      | Must affirm the submission terms             |

Returns `201 Created`:

```json
{
  "referenceCode": "EOTC-XXXXXXXXXXXX",
  "status": "PENDING",
  "message": "Thank you — your submission is awaiting review."
}
```

`GET /api/v1/contributions/status/{referenceCode}` — no login needed; returns the
status (`PENDING`, `NEEDS_CORRECTION`, `APPROVED`, `REJECTED`, `ARCHIVED`), the
church name, depicted figure, image count, and timestamps.

Contributions are rate-limited per client IP
(`RATE_LIMIT_CONTRIBUTE_CAPACITY` per `RATE_LIMIT_CONTRIBUTE_REFILL_MINUTES`); over
the limit returns `429 Too Many Requests`.

## Account

| Method | Path                                            | Description                                  |
|--------|-------------------------------------------------|----------------------------------------------|
| GET    | `/account/submissions`                          | The caller's own submissions and their status |
| GET    | `/account/submissions/{id}`                     | One of the caller's own submissions           |
| PATCH  | `/account/submissions/{id}`                     | Update permitted fields                       |
| POST   | `/account/submissions/{id}/request-correction`  | Request a correction                          |
| POST   | `/account/submissions/{id}/request-removal`     | Request removal                               |

Editable fields are `title`, `description`, `historicalNotes`, `category`,
`captureDate`, `artworkDateStart`, `artworkDateEnd`, `attributionPreference`,
`contributorDisplayName` and `contactEmail`. A submission can only be edited
while it is `PENDING` or `NEEDS_CORRECTION`; after that, use a correction or
removal request. Both request endpoints accept an optional body with `notes`.

## Auth

`POST /api/v1/auth/login`

```json
{ "email": "moderator@example.org", "password": "…" }
```

returns

```json
{
  "accessToken": "…",
  "refreshToken": "…",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Send `Authorization: Bearer <accessToken>` on protected requests. Refresh with
`POST /api/v1/auth/refresh` (`{ "refreshToken": "…" }`); revoke with
`POST /api/v1/auth/logout`. `GET /api/v1/auth/me` returns the current profile.

| Method | Path                | Auth | Description                       |
|--------|---------------------|------|-----------------------------------|
| POST   | `/auth/login`       | —    | Obtain a token pair               |
| POST   | `/auth/refresh`     | —    | Rotate refresh token              |
| POST   | `/auth/logout`      | —    | Revoke a refresh token            |
| GET    | `/auth/me`          | ✓    | Current user profile              |
| GET    | `/auth/health`      | —    | Liveness probe                    |

## Moderation (MODERATOR or ADMIN)

| Method | Path                                | Description                                     |
|--------|-------------------------------------|-------------------------------------------------|
| GET    | `/admin/submissions`                | Queue (`status` defaults to `PENDING`, `page`, `size`) |
| GET    | `/admin/submissions/{id}`           | Full detail incl. private contact email and review history |
| POST   | `/admin/submissions/{id}/decision`  | Apply a review decision                          |

Decision body:

```json
{ "action": "APPROVE", "notes": "Optional reviewer note" }
```

`action` is one of `APPROVE`, `REJECT`, `REQUEST_CORRECTION`. `APPROVE` publishes the
icon and its church/saint and sets `published_at`. Missing/expired tokens yield `401`;
an authenticated user without the role yields `403`.
