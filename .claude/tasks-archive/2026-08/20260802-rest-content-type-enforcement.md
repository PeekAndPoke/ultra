# REST routes accept any Content-Type, so a cross-origin form POST reaches a handler

**Status:** **DONE 2026-08-02** — fixed, and through `/feature-review` (gate **PASS**; the record below
covers this diff *and* increment 1 of `20260719-token-storage-hardening.md`, which was reviewed with it).
**Security-critical:** yes — it is what makes a JSON API CSRF-resistant, and until this landed nothing did.
**Found:** while designing the (since dropped) cookie transport in
`.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`. **It is not a cookie concern** — it is live today.

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

## Fixed 2026-08-02

`passesBodyContentType()` in `funktor/rest/src/jvmMain/kotlin/routing.kt`, called at both body sites
before `call.receive`. Matches `application/json` explicitly rather than via `ContentType.match`, which
treats `*/*` as a match — an absent or wildcard header must be refused, not waved through. Parameters
such as `charset` are accepted. Responds 415.

**The audit came back clean:** no route takes multipart or form-encoded. The only multipart in the tree is
outbound email MIME in `AwsSesSender`. Both real clients already send the header — `ultra/remote` defaults
`contentType = "application/json"`, and the generated TS sets it whenever there is a body
(`runtime/client.ts:151-154`).

### The test harness was lying, and that is the interesting part

Turning the gate on failed ~20 e2e tests. The harness *looked* correct — it appended
`Content-Type: application/json` in a `headers { }` block — but Ktor's `setBody(String)` sets
`text/plain` for a String body, and the appended header does not win. **So every body-bearing e2e test
had been exercising the exact simple-request shape this gate exists to reject**, and nothing noticed
because nothing looked.

Fixed to `contentType(ContentType.Application.Json)` in `funktor/testing/.../AppUnderTest.kt`, which is
what a real client does. Worth remembering as a harness-fidelity trap: a test that constructs its request
differently from every production client is not testing the production path.

`BodyContentTypeSpec` asserts all four directions — `text/plain` and form-urlencoded refused, plain JSON
and JSON-with-charset accepted. Mutation-tested: removing the gate kills both rejection rows.

672 tests green across six modules; compile sweep clean on jvm and js.

## Review record — `/feature-review`, 2026-08-02

Three Opus reviewers over `a0bef940`, `5026e436`, `b61d6d55`, `84d316f9`. Preconditions: sweep clean,
674 tests green. **Gate PASS.** Confirmed findings fixed in `4aa6b808`.

| Severity | Finding | Verdict |
|---|---|---|
| MEDIUM | `verify()` on the sign-in path throws a non-`AuthError` → 500 **after** sign-up has committed the account, password record and activation marker | **CONFIRMED**, fixed — wrapped as `AuthError` |
| MEDIUM | 415 was the only REST error not in an `ApiResponse` envelope; the generated SDK reports such a body as a proxy error with the text withheld | **CONFIRMED**, fixed |
| MEDIUM | `bearerToken` used `as?` against a single-variant sealed type — total, yet nullable, and the shape that silently returns null when a second variant lands | **CONFIRMED**, fixed — exhaustive `when`, non-null, ~20 `!!` removed |
| MEDIUM | docs-site publishes a `generateJwt` example returning the deleted `AuthSignInResponse.Token` | **CONFIRMED**, fixed |
| MEDIUM | The codegen agent's live handoff still instructed a `cookie` variant, a `POST /logout` from us, and non-null `expiresAt` | **CONFIRMED**, fixed |
| MEDIUM | Comma-joined `Content-Type` bypasses the gate | **UNPROVEN** — see below |
| MEDIUM | "The harness was lying" lesson is false | **REFUTED by re-measurement** — see below |
| LOW | Gate ran after param conversion in `handleWithBodyAndParams`, so a refused request cost `findById` reads | **CONFIRMED**, fixed |
| LOW | `userId` derived from `sub` rather than `extractUserData` | **CONFIRMED**, fixed |
| LOW | Missing test rows for absent header and `*/*` — the two cases the KDoc singles out | **CONFIRMED**, added |
| LOW | Cookie KDocs left behind in `AuthState` / `AuthSignInResponse` | **CONFIRMED**, fixed |
| LOW | FQCN in `JwtPayloadNumericClaimsSpec` | **CONFIRMED**, fixed |

### The two claims that were wrong, and whose

**Mine.** I reported a comma-joined `Content-Type` as a *confirmed* fail-open, having inferred it from a
403 instead of a 415. I never checked what the server received. It receives a clean `application/json` —
ktor's **client** normalises the value before sending, so the harness cannot construct the attack at all.
The bypass is unproven; a raw socket or a header-joining proxy might still produce it, untested. The
single-line/no-comma hardening stays because RFC 9110 §8.3 makes a list malformed regardless, but the
test row asserting 415 was removed: it asserted a scenario that cannot occur.

**A reviewer's.** The "harness was lying" lesson was refuted with a MockEngine probe showing
`headers { append(...) }` wins. Re-measured inside the real harness by logging what the server receives:
`/auth/non-existent/signup` gets `text/plain; charset=UTF-8`, and reverting the harness fails 30 tests.
The lesson holds — but only for ktor's **test** client; the reviewer's bare `HttpClient` genuinely
behaves differently. That distinction is the accurate version and is now what the doc says.

### Not fixed — recorded, with reasons

- **`RefreshToken` is a `GET` that mints a token** (`AuthApiClient.kt:95`, `AuthUserApi.kt:47`). A GET has
  no body, so the Content-Type gate can never apply. Harmless under bearer, since it needs an
  `Authorization` header — but it is exactly the landmine this task exists for. **Make it a POST before
  any ambient credential ever lands.** Changing it now would break the generated SDK for no present gain.
- **Bodiless mutating routes bypass the gate.** `ApiRoutes.post(uri): ApiRoute.Plain` exists
  (`ApiRoutes.kt:307`) and is ungated, because the gate hangs off the two body-reading handlers.
  Unreachable today — `TypedApiEndpoint.Post/Put.mount` only ever produce `WithBody`/`WithBodyAndParams`,
  and no `Plain` POST exists in the tree — so the invariant is disciplinary, not structural. Gating on
  METHOD inside `Route.handle` would make it structural.
- **`.claude/tasks-archive/2026-07/20260726-client-jwt-claims.md`** describes `jwtClaims.kt` and `Data.claims`, both
  deleted. Should be closed out and archived.
