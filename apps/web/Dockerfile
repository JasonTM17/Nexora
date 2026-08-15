# syntax=docker/dockerfile:1.7
# Multi-stage build for Nexora web (Next.js 16 / React 19).
# Runtime: non-root, distroless-compatible, HEALTHCHECK, OCI labels.

# ---- Build stage ----
FROM node:24.12-alpine AS build
WORKDIR /src

# Install dependencies (frozen lockfile)
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml .npmrc ./
COPY apps/web/package.json ./apps/web/
COPY packages/contracts/package.json ./packages/contracts/
COPY packages/design-tokens/package.json ./packages/design-tokens/
COPY packages/ui-core/package.json ./packages/ui-core/
COPY packages/ui-studio/package.json ./packages/ui-studio/
COPY packages/ui-ai/package.json ./packages/ui-ai/
COPY packages/ui-builder/package.json ./packages/ui-builder/
RUN corepack enable && corepack prepare pnpm@11.0.9 --activate \
    && pnpm install --frozen-lockfile

# Build the application
COPY . .
RUN cd apps/web && pnpm exec next build

# ---- Runtime stage ----
FROM node:24.12-alpine AS runtime
WORKDIR /app

# Non-root user
RUN addgroup -S nexora && adduser -S nexora -G nexora
USER nexora:nexora

# Copy built application
COPY --from=build --chown=nexora:nexora /src/apps/web/.next/standalone ./
COPY --from=build --chown=nexora:nexora /src/apps/web/.next/static ./.next/static
COPY --from=build --chown=nexora:nexora /src/apps/web/public ./public

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
