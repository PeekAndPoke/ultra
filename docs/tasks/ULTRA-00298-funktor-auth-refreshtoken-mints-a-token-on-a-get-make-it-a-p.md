# ULTRA-00298: funktor-auth: RefreshToken mints a token on a GET; make it a POST

> **Status (2026-11-08): DESIGNED.** The maintainer, 2026-11-08: "This should not be a GET request then." No
> decisions open; goes with or after ULTRA-00299 (every POST needs Content-Type application/json).
>
> **Part of:** ULTRA-00248 (Epic: Auth and sessions)

**Plan:** none. Recorded as a residual in the archived ULTRA-00185 (rest content type enforcement).
**Security-critical:** yes (CSRF hardening). Red team: the scenario goes into ULTRA-00187 (redteam sdk auth).

## Why this exists

`AuthApiClient.RefreshToken` is a `TypedApiEndpoint.Get` (`funktor/auth/src/commonMain/kotlin/api/AuthApiClient.kt:95`),
mounted in `AuthUserApi.kt:47`. It issues a fresh token. A GET should be safe: browsers send it cross-site without a
preflight, `SameSite=Lax` cookies ride along on top-level GETs, and caches and logs treat GET responses as shareable.
Today it is harmless, because the call needs an `Authorization` header that a foreign page cannot set. It stops being
harmless the day any ambient credential appears (a cookie, a proxy that injects auth).

## The work

- [ ] `RefreshToken` becomes a POST. Either it gets a body (an empty request object), or it stays bodiless once
      ULTRA-00299 gates bodiless POSTs. Without ULTRA-00299, a bodiless POST would bypass the Content-Type gate.
- [ ] Callers follow: `AuthApiClient.kt:152`, `AuthState.doRefreshToken` (`jsMain/AuthState.kt:398`), the generated TS
      SDK's auth runtime (regenerate, and diff the generated tree), and the docs, `llms/funktor.md` and the CHANGELOG
      (breaking).
- [ ] Audit: any other GET in funktor that changes state or issues a credential. List what was checked, even when
      nothing is found.

## Acceptance

- [ ] E2e: POST refreshes; GET on the same path no longer does (405 or 404, whichever the router gives; pin it).
- [ ] The Kraft `AuthState` refresh test still passes, and the TS SDK test does too.
- [ ] Mutation-checked: put back the GET mount, and the e2e goes red.

## History

- 2026-11-08: created.
