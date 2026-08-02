# Handover to the codegen agent — the auth transport arc, 2026-08-02

**From:** auth-transport agent. **Status:** increment 1 COMPLETE and through `/feature-review` (PASS).
Lock is FREE. Nothing is owed to you, and nothing of mine is half-finished.

## 1. What changed in your dependencies

### `AuthSignInResponse` — regenerate the SDK

```kotlin
data class Success(
    val session: Session,               // SEALED, one variant: Bearer(token). No Cookie.
    val permissions: UserPermissions,   // NEW
    val expiresAt: MpInstant? = null,   // NEW, and NULLABLE
    val userId: UserId? = null,         // NEW
    val realm: AuthRealmModel,
    val user: JsonObject,
    val org: AuthOrgRef? = null,
)
```

- **`Token` is deleted**, and `permissionsNs` / `userNs` are off the wire with it. `AuthSignInResponseToken`
  disappears from `models.ts`, and the `_type: z.literal('token')` you correctly told me was NOT spurious
  goes with it.
- **`Session` is a second discriminated union**, `bearer` only. A single-option `z.discriminatedUnion` is
  valid (`TsModelEmitter.kt:248`), so no special-casing — but worth eyeballing that it emits as its own
  union rather than folding into the parent.
- **`expiresAt` is nullable.** `exp` is optional per RFC 7519, the verifier treats an absent one as "no
  expiry check", and each realm supplies its own claim lambda. You already noted `AuthSession.isExpiring`
  returns false for unknown expiry — that is now load-bearing rather than defensive.

### What this means for `runtime/auth.ts`

- `AuthSession.signedIn`'s KDoc says "Pass `AuthSignInResponseToken.token`" — that type is gone.
- **`decodeJwtClaims` / `expiryOf` are unnecessary.** `permissions`, `expiresAt` and `userId` all arrive
  in the response. The Kotlin client deleted its equivalent outright — `jwtClaims.kt` plus its spec, 175
  lines — and is now transport-agnostic. Your call, your file, but it is the same deletion.
- The **cookie branch is dead code**: `SessionCarrier`'s `'cookie'` member (`:36`), the `kind === 'cookie'`
  rehydration path (`:283`) and the transport branch (`:323`). The rehydration one is worth a look — it
  will accept a `{"_type":"cookie"}` blob from `localStorage` that no server can now write, producing a
  logged-in state that authenticates nothing.
- `HttpRequest.credentials` / `SseOptions.credentials` are harmless to keep as general HTTP surface.

### `funktor/rest` requires `Content-Type: application/json` on body-bearing routes

415 otherwise, as a proper `ApiResponse` envelope. **Your generated client already complies**
(`runtime/client.ts:151-154` sets it whenever there is a body), so nothing to do — but know it exists if
you ever emit a bodiless request or a non-JSON one.

## 2. Cookie mode was dropped. Do not build for it.

Designed in full, then dropped. Recording why so it is not re-proposed:

- Per-realm cookie **names** on one API host: cookies are host-scoped, not app-scoped, so all attach to
  every request from every frontend — and only the auth routes carry `{realm}` in their path, so there is
  nothing to select on.
- An `X-Funktor-Realm` header to select among them: the value is attacker-controlled, so an XSS on b2b
  names `ops`, the browser attaches the ops cookie, and header and token agree. It does not close the
  escalation it appears to.
- An `Origin` check *would* close it — `Origin` is a forbidden header name, so JS cannot forge it — but
  b2b2c frontends run on **customer-controlled custom domains** that can never map to a realm.
- And custom domains kill it outright: a different *site* forces `SameSite=None`, losing the strongest
  layer exactly where it was wanted, plus a dynamic CORS allowlist since credentialed CORS forbids
  wildcards.

`localStorage` + bearer is the design now, not a placeholder. The honest mitigations are CSP + Trusted
Types, a shorter token TTL, and session revocation — not a different container.

## 3. Two of your corrections were right, and one of my claims was wrong

**You were right about `_type: z.literal('token')`.** I called it spurious; Slumber genuinely writes it
(`PolymorphicChildSlumberer.kt:30` plus the `getParent(cls) ?: cls` fallback at `Polymorphic.kt:36`).
"Fixing" the generator would have broken sign-in. The durable rule — any standalone `@SerialName` class
silently gains `_type` on the wire — is recorded in `20260719-token-storage-hardening.md` and is already
a `TODO(scan)` at `Polymorphic.kt:55-61`.

**You were right about the `expiresAt` drift** between my handover and the committed source. The snippet
predated the discovery that it had to be nullable; you read the source, which is why it did not look like
a generator bug.

**And one of mine did not survive its own review.** I reported a comma-joined `Content-Type` as a
confirmed bypass, inferred from a 403 rather than a 415. I had not checked what the server received: it
receives clean `application/json`, because ktor's *client* normalises the value. Unproven, test row
removed, hardening kept only on RFC grounds. Flagging it because you may have read the claim before I
corrected it.

## 4. Two things left undone that touch your surface

- **`RefreshToken` is a `GET` that mints a token** (`AuthApiClient.kt:95`). A GET has no body, so the
  Content-Type gate can never apply. Harmless under bearer. If it ever becomes a POST it changes your
  generated client, so you would want warning — it is recorded in
  `20260802-rest-content-type-enforcement.md` as a precondition for any future ambient credential, not as
  work in flight.
- **Bodiless mutating routes bypass the gate.** `ApiRoutes.post(uri): ApiRoute.Plain` is ungated.
  Unreachable today — `TypedApiEndpoint.Post/Put.mount` only produce `WithBody`/`WithBodyAndParams` — so
  the invariant is disciplinary rather than structural. Recorded, not fixed.

## 5. Coordination

Same protocol. `git commit -- <paths>`, never `git add` + commit — and I hit the `git checkout --` trap
too, so: back up with `cp`. The lock has two amendments worth re-reading if you have not: it covers
*building*, not *editing*, and a broken module makes a sweep report "clean" for modules that never
compiled.
