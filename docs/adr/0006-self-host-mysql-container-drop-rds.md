# ADR 0006: Self-host MySQL in a container on EC2; drop managed RDS

## Status

Accepted — amends ADR 0005 (its RDS choice is superseded; the Graviton/free-trial and Lightsail-lifecycle parts of 0005 still stand).

## Context

ADR 0005 kept managed **RDS** on the deployment even though it is the only line item that costs
real money (`db.t3.micro`, single-AZ, ~$13/month), justified by a single premise: recruiter/ATS
résumé screens filter on the "RDS" keyword, so keyword value outweighs the cost.

Two facts break that premise for this project:

1. **The data has no value.** The database only holds demo content that is re-seeded by Flyway +
   demo seeding on every backend boot (`docker-compose.prod.yml`, `start_period: 90s`). It can be
   destroyed and rebuilt at will. This zeroes out RDS's real value-adds — automated backups,
   point-in-time recovery, managed failover.
2. **"RDS" is not a keyword in the target market.** Surveying Ireland job postings, the database/
   cloud keywords that actually appear are `AWS`, `Docker`, `ECS`, `Kubernetes`, `PostgreSQL`,
   `S3`, `Lambda`, `DynamoDB`, `Kafka` — **not** "RDS". The "AWS deployment" story is carried by
   EC2 + ECR + VPC + Security Groups regardless of where the database runs, and the "database"
   keyword is the engine (MySQL/PostgreSQL), not the managed wrapper.

With both value-adds gone and the keyword premise empirically false, paying (or burning free
credits) for a managed database on a personal résumé demo is not justified.

## Decision

Drop RDS from the running deployment and self-host **MySQL in a Docker container on the same EC2
`t4g.small` host**, on the compose internal network with no host port mapping.

- Add a `mysql` service back to `docker-compose.prod.yml` (as the local `docker-compose.yml`
  already does), reached by the backend over the compose network via `BOOKINN_DB_URL`.
- Tear down the RDS instance (`aws rds delete-db-instance ... --skip-final-snapshot`).
- No RDS screenshots are captured; instead the RDS setup is documented in plain language
  (`~/document/rds-hosting-review.html`) for interview recall.

### What this buys / costs

- **Running cost → $0.** EC2 is already free via the Graviton trial through 2026-12-31; no
  separate database bill and no free-credit burn.
- **Data isolation is unchanged or better.** The container is not published to the host, so the DB
  is even less reachable than RDS-in-a-private-subnet; the "DB unreachable from the internet"
  acceptance criterion still holds.
- **Interview framing is a net positive.** "Why not RDS?" has a strong answer: cost control on a
  personal demo; managed RDS would be the production choice. Demonstrates situational trade-off
  judgement rather than absence of RDS knowledge.
- **Trade-off accepted:** the EC2 host (2 GB RAM) now runs nginx + JVM + MySQL together, so the
  MySQL container should get a modest memory cap; and the loss of managed backups is acceptable
  because the data is disposable and re-seeded on boot.

## Consequences

- `docker-compose.prod.yml`'s current comment ("RDS MySQL — deliberately NOT in this file") and the
  RDS steps in `docs/deploy/aws-setup.md` (§3 RDS SG, §4 create-db-instance) must be updated to the
  container model. Implementation is left to the user per the repo's agent-role convention.
- The Lightsail-migration step in ADR 0005 becomes trivial — the DB is already a container, so the
  same `docker-compose.prod.yml` moves as-is.
- The unresolved higher-leverage keyword mismatch — the project uses **MySQL** while target JDs ask
  for **PostgreSQL** — is explicitly out of scope here and deferred to a separate decision.
