# ADR 0002: Pessimistic row locking to prevent double-booking

## Status

Accepted

## Context

Creating a booking is a check-then-act sequence: query existing `CONFIRMED` bookings for date overlap, then insert if none is found. Two requests for the same listing and overlapping dates can both pass the check before either inserts, producing a double booking. Several fixes exist at different points on the cost/guarantee spectrum:

- **Optimistic locking** (`@Version` on the listing) only detects concurrent *updates* to the same row. Two booking creations are two separate `INSERT`s, not competing updates to the listing — a version check does not observe the conflict at all.
- **A distributed lock (e.g. Redis)** solves this for multiple *application instances* sharing no other coordination point. This system is a single-instance monolith with one database; the database itself is already the shared coordination point, so an external lock adds infrastructure to solve a problem the database already solves.
- **A database-level exclusion constraint** (one row per booked night, unique on `(listing_id, night_date)`) gives the strongest guarantee — it blocks even writes that bypass the application entirely — at the cost of a second table to maintain and range logic to translate constraint violations into a user-facing error.

## Decision

Wrap booking creation in a transaction that takes `@Lock(PESSIMISTIC_WRITE)` on the target `Listing` row before running the overlap query and the insert. This serializes concurrent booking attempts *for the same listing* — different listings are never blocked by each other — and the lock is released when the transaction commits or rolls back.

## Consequences

- Correctness holds for this system's actual failure mode (two requests, one process, one database) with a single annotation and no new infrastructure.
- Throughput for concurrent bookings on the *same* listing is serialized rather than parallel — an explicit non-goal here, since booking volume per listing is inherently low-frequency (nobody books the same cabin twice a second).
- Not evaluated: an exclusion-constraint table, which would additionally guard against writes that skip the application layer. Out of scope for a single-application system, but the natural next step if a second write path (e.g. an admin tool or a second service) is ever introduced.
