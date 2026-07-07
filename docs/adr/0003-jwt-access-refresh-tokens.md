# ADR 0003: JWT access + refresh tokens, refresh in an httpOnly cookie

## Status

Accepted

## Context

The frontend is a separate SPA calling a stateless REST API, which rules out server-side sessions as the natural fit — the backend would need to hold session state that a horizontally-scaled or stateless deployment shouldn't require. A single long-lived JWT is the simplest alternative, but it creates a hard trade-off: long expiry means a stolen token stays valid for a long time with no way to revoke it; short expiry means the user is logged out constantly.

Splitting the token also raises where to store the piece that matters most for security — the token that can mint new access tokens. `localStorage` is readable by any JavaScript running on the page, so an XSS vulnerability anywhere in the frontend leaks it directly. A cookie is not readable by JavaScript if marked `httpOnly`, at the cost of needing CSRF protection instead (since the browser attaches cookies to requests automatically, a malicious site can trigger authenticated requests it can't read the response of).

## Decision

Two tokens: a short-lived access token (15 min), returned in the response body and held in memory on the frontend, and a longer-lived refresh token (7 days), set as an `httpOnly` cookie and rotated (invalidated and reissued) on every use. Logout revokes the current refresh token server-side. The frontend's Axios layer transparently retries a request once on a 401 by calling `/api/auth/refresh` first.

## Consequences

- An XSS exploit can steal the in-memory access token (15-minute blast radius) but cannot read the refresh token — it's not exposed to JavaScript at all.
- CSRF becomes the relevant threat model for the refresh endpoint instead of XSS-driven token theft; mitigated with `SameSite=Strict` on the cookie, which is sufficient here since there's no cross-site form-submission flow that legitimately needs the cookie sent cross-origin.
- Rotation means a stolen refresh token is single-use from the attacker's perspective once the legitimate client refreshes again — but it also means refresh calls must be handled carefully on the frontend to avoid two concurrent tabs invalidating each other's token.
