# BookInn

A full-stack vacation rental booking platform inspired by Airbnb — built to work through booking concurrency, role-based access control, and a real end-to-end deployment pipeline.

🚧 **In active development.** Progress is tracked module-by-module on the [project board](https://github.com/users/Jamiedz999/projects/1).

## Tech stack

**Backend** — Java 21 · Spring Boot 3 · Spring Security (JWT) · Spring Data JPA · MySQL · Flyway · springdoc-openapi · JUnit 5 / Mockito · Testcontainers

**Frontend** — React 18 · TypeScript · Vite · TanStack Query · MUI · Recharts · Vitest

**Infra** — Docker · AWS (EC2, RDS, ECR) · GitHub Actions

## Highlights

- JWT authentication with access/refresh token rotation and role-based authorization (guest / host)
- Concurrency-safe booking creation under row-level locking — [ADR 0002](docs/adr/0002-pessimistic-locking-for-double-booking.md)
- Host dashboard with real-time booking and revenue analytics
- Containerized deployment to AWS behind a CI/CD pipeline that enforces test coverage gates

## Design decisions

Architecture and design trade-offs are recorded as ADRs in [`docs/adr/`](docs/adr/).

## Development

Built as a series of independently verifiable modules, tracked as GitHub issues on the [project board](https://github.com/users/Jamiedz999/projects/1).

---

*Screenshots, a live demo link, and local setup instructions will be added as the first modules land.*
