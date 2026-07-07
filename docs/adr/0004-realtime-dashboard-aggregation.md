# ADR 0004: Real-time aggregation for the host dashboard (no pre-aggregated tables)

## Status

Accepted

## Context

The host dashboard shows revenue trends and occupancy rates, both of which require aggregating across a host's bookings. There are two standard approaches: compute the aggregation on read (a query runs when the dashboard loads), or compute it on write (a scheduled or triggered job maintains pre-aggregated summary tables that reads hit instead). Pre-aggregation is the standard answer once query volume or data volume makes on-read computation too slow — but it adds a second source of truth that can drift from the underlying data, and a job to keep it in sync.

## Decision

Every dashboard endpoint (`summary`, `revenue-trend`, `occupancy`) runs a JPQL/native aggregate query against the live `booking` table on each request, returned as DTO projections. No summary tables, no background aggregation job.

## Consequences

- One source of truth; a dashboard number can never disagree with the bookings it's computed from.
- Query correctness (not query performance) is the interesting engineering problem here — e.g. the occupancy query has to clip a booking's date range to the intersection with the queried month, and the revenue trend has to zero-fill months with no bookings rather than silently omitting them from a `GROUP BY`.
- This does not scale indefinitely: at a host-count and booking-volume this project will never reach, on-read aggregation would need to move to pre-aggregated tables (refreshed on write or on a schedule) or a read-optimized store. That migration is a known, deferred next step, not an oversight.
