# syntax=docker/dockerfile:1.7
# Multi-stage build for Nexora web (Next.js 16 / React 19).
# Runtime: non-root, standalone output, HEALTHCHECK, OCI labels.

# ---- Build stage ----
FROM node:24.12-alpine AS build
WORKDIR /src

# Copy ALL source first (workspace packages need their source for build)
COPY . .

# Install dependencies (frozen lockfile)
RUN corepack enable && corepack prepare pnpm@11.0.9 --activate \
    && pnpm install --frozen-lockfile

# Build the web app
RUN pnpm --filter @nexora/web build

# ---- Runtime stage ----
FROM node:24.12-alpine AS runtime
WORKDIR /app

# Next's standalone output preserves the monorepo app path (`apps/web`).
# Copy the whole traced tree, then run from the directory containing server.js.
COPY --from=build /src/apps/web/.next/standalone ./
COPY --from=build /src/apps/web/.next/static ./apps/web/.next/static
WORKDIR /app/apps/web

# Non-root user (create after copying)
RUN addgroup -S nexora && adduser -S nexora -G nexora \
    && chown -R nexora:nexora /app
USER nexora:nexora

ENV NODE_ENV=production
ENV PORT=3000
EXPOSE 3000

# HEALTHCHECK
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=6 \
  CMD wget -qO- http://127.0.0.1:3000/healthz || exit 1

# OCI labels
LABEL org.opencontainers.image.source="https://github.com/JasonTM17/Nexora"
LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.title="Nexora Web"
LABEL org.opencontainers.image.description="Nexora tenant-aware CMS and knowledge workspace web surface"

ENTRYPOINT ["node", "server.js"]
