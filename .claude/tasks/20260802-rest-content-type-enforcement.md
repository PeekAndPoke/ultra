# REST routes accept any Content-Type, so a cross-origin form POST reaches a handler

**Status:** FOUND 2026-08-02, NOT FIXED. Small and self-contained.
**Security-critical:** yes — it is what makes a JSON API CSRF-resistant, and right now nothing does.
**Found:** while designing the (since dropped) cookie transport in
`.claude/tasks/20260719-token-storage-hardening.md`. **It is not a cookie concern** — it is live today.

## What happens

`funktor/rest/src/jvmMain/kotlin/routing.kt:205,234` does `call.receive<ByteArray>()` and hands the bytes
to `restCodec.deserialize(...)`. **No Content-Type is checked, anywhere.** So a request declaring
`text/plain` — or nothing at all — with a JSON body is parsed and dispatched exactly like a real one.

## Why it matters

A **simple request** (POST with `text/plain`, `application/x-www-form-urlencoded`, or `multipart`) is
sent by the browser with **no CORS preflight**. CORS then governs only whether the *response* is readable.
The side effect has already happened.

So a cross-origin page can do:

```html
<form method="POST" action="https://api.example.com/some/route" enctype="text/plain">
```

and reach a handler. A JSON API is normally immune to this by accident, because `application/json` is not
a simple content type and therefore always preflights. This one is not immune, because it never checks.

**Today the exposure is limited**, since bearer tokens are not ambient — an attacker page cannot set
`Authorization`, so the request arrives anonymous and dies at the auth floor. The hole matters because:

1. Any route with a `public()` floor and a side effect is reachable cross-origin right now.
2. It is a landmine for any future ambient credential. The cookie design was dropped, but an API key in a
   cookie, a session cookie added by an app, or a proxy that injects auth would all step on it.

## The fix

Reject a body whose Content-Type is not `application/json` (allowing a charset parameter, and an absent
body for GET/DELETE). Two call sites.

- [ ] Enforce at `routing.kt:205` and `:234`, before `restCodec.deserialize`.
- [ ] **Audit for routes that legitimately take something else first** — file upload is the usual
      exception and would need `multipart/form-data` allowed explicitly, per route rather than globally.
- [ ] Return 415 Unsupported Media Type, not 400: it is the accurate status and it tells a real client
      what to fix.
- [ ] Test both directions. A test that only asserts JSON still works proves nothing — the point is that
      `text/plain` is now refused.

## Related, and NOT fixed by this

`SameSite` is irrelevant while there is no cookie, but note for whenever one appears: `SameSite=Lax`
still attaches a cookie to top-level **GET** navigations, so a state-changing GET stays forgeable.
REST hygiene says there are none; confirm rather than assume.
