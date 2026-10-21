# API Reference

Base URL: `/api/v1`. Interactive docs are served at `/swagger-ui.html`
(OpenAPI JSON at `/api-docs`).

- All responses are JSON (`application/json`) unless noted.
- Timestamps are ISO-8601 in UTC.
- IDs are UUIDs unless stated otherwise.
- Errors use Spring's problem-style body with `status`, `error`, and `message`
  (messages are not stack traces — `server.error.include-stacktrace=never`).

## Authentication

The archive is public. Only `/api/v1/admin/**` and `/api/v1/account/**` need a token.

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

## Icons (public)

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

## Churches (public)

| Method | Path                        | Description                                  |
|--------|-----------------------------|----------------------------------------------|
| GET    | `/churches`                 | Directory search (`q`, `region`, `sort`, `page`, `size`) |
| GET    | `/churches/{idOrSlug}`      | Church detail with its published icons       |
| GET    | `/churches/map/markers`     | Churches with usable coordinates for the map |

## Saints (public)

| Method | Path                    | Description                                             |
|--------|-------------------------|---------------------------------------------------------|
| GET    | `/saints`               | Directory search (`q`, `category`, `sort`, `page`, `size`) |
| GET    | `/saints/{idOrSlug}`    | Saint detail with depictions and churches               |

## Archive metadata (public)

| Method | Path                      | Description                                    |
|--------|---------------------------|------------------------------------------------|
| GET    | `/stats`                  | Published counts (icons, churches, saints, regions) |
| GET    | `/regions`                | Distinct regions in the published archive      |
| GET    | `/categories`             | Distinct icon categories                       |

## Contributions (public, no login)

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

## Account (optional, authenticated)

| Method | Path                      | Description                                  |
|--------|---------------------------|----------------------------------------------|
| GET    | `/account/submissions`    | The caller's own submissions and their status |

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
