# ADR 0005: Stay on AWS for the deployment highlight; manage cost with Graviton free trial + a 6-month → Lightsail lifecycle

## Status

Accepted

## Context

M7 deploys the app to a public cloud. AWS is one of the project's five résumé highlights, but the AWS Free Tier was overhauled on 2025-07-15: new accounts no longer get the 12-month "750 free micro-hours" — they get $100–$200 in credits over a hard 6-month window, after which the account closes (Free plan) or bills pay-as-you-go (Paid plan). The PRD's stack (`t3.small` + RDS `db.t3.micro`, 24/7) costs ~$36/month, so it is no longer free to run long-term on AWS.

Oracle Cloud's Always Free tier (2 OCPU / 12 GB Arm + MySQL HeatWave, $0 forever) was considered as a replacement and rejected: the deployment concepts transfer 1:1 (Compute↔EC2, VCN Security List↔Security Group, OCIR↔ECR, managed MySQL↔RDS), but the literal "AWS" keyword — which is what recruiter/ATS résumé screens filter on — does not. Keyword value outweighed the cost saving.

## Decision

Deploy M7 on AWS as the PRD describes, with three cost measures layered on:

1. **Compute on Graviton.** Use `t4g.small` (Arm, 2 vCPU / 2 GB — same specs as `t3.small`) instead of `t3.small`. Its free trial gives 750 hrs/month (24/7) at $0 through 2026-12-31 for all accounts, stacking on top of the credits. This makes EC2 compute effectively free for the rest of 2026 and leaves the $200 credits to absorb RDS.
2. **Bill guardrails.** Root-account MFA + zero leaked access keys (the real "surprise bill" is a leaked key mining crypto, not the demo), AWS Budgets alerts at $1/$5/$10 plus a zero-spend alert, and a Budget Action that auto-stops EC2 at the threshold. Never create a NAT Gateway; single-AZ RDS only.
3. **6-month → Lightsail lifecycle.** Run EC2 + RDS + ECR for the ~6-month credit window (rich EC2/RDS/VPC/Security-Group story + screenshots + README architecture). When the window ends, if the live demo is still needed, migrate the always-on demo to Lightsail (fixed ~$12–27/month, no surprise-bill surface); otherwise take it down and rely on `docker compose up` + screenshots. The migration is near-free because everything is containerized — the same `docker-compose.prod.yml` runs on a Lightsail box.

## Consequences

- Docker images must be built for **arm64** (backend JRE base + nginx), and CI (M8) must cross-build via `docker buildx`/QEMU or an Arm runner. This is on-strategy: "multi-arch image build + Graviton deploy" deepens the Docker and CI/CD highlights rather than being a pure cost hack.
- The demo's guaranteed lifetime is ~6 months on the free credits; the README should state the demo's validity window and always offer the local `docker compose up` path as the durable fallback.
- This supersedes the PRD's `t3.small` choice for M7/§3. The PRD remains source of truth and should be patched to match.
