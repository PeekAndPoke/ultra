# ULTRA-00299: funktor-rest: every POST needs Content-Type application/json, with a body or without

> **Status (2026-11-08): DESIGNED.** The maintainer, 2026-11-08: "Do we need the content-type check, when there is no
> body? If this is good practice, then we will do it." Answer: yes (below). No decisions open.
>
> **Part of:** ULTRA-00251 (Epic: Funktor framework)

**Plan:** none. Recorded as a residual in the archived ULTRA-00185 (rest content type enforcement).
**Security-critical:** yes (CSRF). Red team: the scenario goes into ULTRA-00058 (redteam error disclosure) or the
red-team ticket that ULTRA-00185 fed. Decide on archive.

## Why this exists, and why "no body" doesn't make it unnecessary

The gate in `funktor/rest/src/jvmMain/kotlin/routing.kt:125` (`passesBodyContentType`) is not about parsing the body.
It forces a **CORS preflight**. A browser sends a cross-site request without asking first only when it is a "simple
request":
- the method is GET, HEAD or POST;
- the Content-Type, if there is one, is `text/plain`, `application/x-www-form-urlencoded` or `multipart/form-data`;
- there are no custom headers.

Requiring `application/json` makes every request that passes the gate non-simple. So a foreign page has to pass the
preflight, which means the CORS allowlist, before it can reach the handler.

A POST **without a body** is the textbook simple request: `fetch(url, {method: "POST", credentials: "include"})` from any
site arrives with no preflight. The gate hangs only on the two body-reading handlers (`routing.kt:263,294`), so
`ApiRoutes.post(uri): ApiRoute.Plain` (`ApiRoutes.kt:307`) is ungated. No such route exists today, but nothing stops one.
Two things make it reachable:
- `public()` routes with side effects, today;
- any ambient credential (a cookie), later.

PUT, PATCH and DELETE always preflight, whatever their headers. POST is the only state-changing method at risk, but one
rule for every unsafe method is simpler to reason about and to test.

This is established practice: OWASP's CSRF cheat sheet lists a required custom request header for APIs, and a
required non-simple Content-Type works the same way.

## The work

- [ ] Gate on the METHOD, not on the handler. For every request that is not GET, HEAD or OPTIONS, require exactly one
      `Content-Type: application/json` header (same rules as now: one line, no comma, wildcards refused), body or no
      body. It must be structural, in the one place every API route passes through, so a new route kind cannot miss
      it.
- [ ] Clients always send the header on those methods, even without a body:
  - the Kotlin `ultra/remote` client (`RemoteRequestImpl.kt:55`);
  - the generated TS SDK runtime;
  - the Vue/TS contributors that call `fetch` directly, if any.
- [ ] `ApiRoute.Plain` POSTs and bodiless PUT/DELETE in the tree keep working through those clients.
- [ ] Docs: the funktor-rest page and `llms/funktor.md`, one paragraph on the rule and why. CHANGELOG (breaking for
      hand-written clients that omit the header).

## Acceptance

- [ ] E2e: a bodiless POST without the header is 415; with it, it passes. The same for DELETE and PUT. GET is not
      affected.
- [ ] E2e through the real Kotlin client and through the TS SDK test harness: bodiless POST and DELETE pass.
- [ ] Mutation-checked: put the gate back on the body handlers only, and the bodiless test goes red; let a missing
      header through, and it goes red.

## History

- 2026-11-08: created.
