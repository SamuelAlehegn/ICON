# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage: install from the lockfile and produce the production bundle.
# Node 22 matches the version used by the frontend CI workflow.
# ---------------------------------------------------------------------------
FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# The `application` builder emits the SPA into dist/eotc-icon-ui/browser.
RUN npm run build -- --base-href /

# ---------------------------------------------------------------------------
# Runtime stage: Nginx serves the bundle and proxies the API to the backend.
# ---------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/eotc-icon-ui/browser /usr/share/nginx/html/browser

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=10s --retries=5 \
    CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
