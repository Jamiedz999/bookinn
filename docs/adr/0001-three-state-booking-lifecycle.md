# ADR 0001: Three-state booking lifecycle (Instant Book, no host approval)

## Status

Accepted

## Context

A booking needs a lifecycle: something has to represent "this reservation is live," "it happened," and "it was called off." The natural model to reach for is Airbnb's own flow — a `PENDING` state where the host reviews and accepts or rejects the request before it's confirmed. That model needs a full approval UI for hosts, a timeout/expiry job for requests nobody answers, and a rule for whether a `PENDING` request soft-blocks the calendar while it waits. Each of those is its own slice of work, and none of it changes what the rest of the system (search, pricing, cancellation) needs to reason about.

## Decision

Bookings use three states: `CONFIRMED → COMPLETED → CANCELLED`, with `COMPLETED` and `CANCELLED` as terminal. A booking is instantly `CONFIRMED` on creation (subject to the availability and locking rules in ADR 0002) — there is no host review step. A daily scheduled job transitions any `CONFIRMED` booking whose checkout date has passed to `COMPLETED`. Guests can cancel a `CONFIRMED` booking up to 48 hours before check-in; any transition out of a terminal state is rejected.

## Consequences

- No host-approval UI, no request-expiry job, no "does a pending request block the calendar" question to answer.
- `COMPLETED` is a real, persisted state (not derived at query time), driven by a scheduled job — this keeps eligibility checks (e.g. "only reviewable after completion") a simple state check rather than a date computation repeated everywhere.
- Trade-off: hosts have no manual gate on who books their listing. Real platforms often support both instant-book and request-to-book; this project deliberately picks one to keep the state machine and its tests small.
