# syntax=docker/dockerfile:1
#
# Multi-stage build for the React frontend.
#   Stage 1 (build)   — Node compiles the Vite production bundle.
#   Stage 2 (runtime) — nginx serves the static bundle and reverse-proxies /api to the backend.
# Built for linux/arm64 to run on the Graviton (t4g) EC2 host — see ADR 0005.

# ---- build stage ----
FROM node:20-alpine AS build
WORKDIR /app

# Install deps first (cached until the lockfile changes). npm ci = clean, reproducible install.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- runtime stage ----
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
