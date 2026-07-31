# RED TEAM — our own JWT verifier (post java-jwt removal)

**Status:** COLLECTED — do not execute here. For a dedicated penetration-test session.
**Created:** 2026-07-31
**Plan:** `.claude/tasks/20260718-jwt-lib-consolidation-options.md` (RESOLVED section)
**Why:** `com.auth0:java-jwt` was removed on 2026-07-31 and this codebase now owns JWT signing,
signature verification and registered-claim validation outright. Every request bearing an
`Authorization: Bearer` header hits this code, unauthenticated.

## The code under attack

| File | Role |
|---|---|
| `ultra/security/src/jvmMain/kotlin/jwt/JwtSignatureGate.kt` | length cap, shape check, HMAC-SHA512 over raw segments, constant-time compare |
| `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt` | `verify` order, payload decode, `validateClaims`, `sign` |
| `ultra/security/src/jvmMain/kotlin/jwt/JwtBuilder.kt` | claim assembly / wire shapes |
| `ultra/security/src/jvmMain/kotlin/jwt/extract.kt` | claims → `UserId` / `UserPermissions` |
| `funktor/rest/src/jvmMain/kotlin/auth/jwtCaller.kt` | the ktor entry point (`tryJwtCaller`) |

## Scenarios to attempt

### A. Forge a token without the key
1. **Signature bypass by shape.** Tokens with extra dots, trailing dots, empty segments, a signature
   segment that base64url-decodes to zero bytes, and unicode/whitespace-padded segments. The gate
   uses `lastIndexOf('.')` and `substring` — try to make signing input and payload disagree about
   which bytes were authenticated. **Specifically: can a 4-segment token pass the gate (which only
   looks at the LAST dot) and then have `decodeClaims` read a different segment than the one
   covered by the MAC?** `decodeClaims` rejects `segments.size != 3`, so verify that holds.
2. **Base64url malleability.** The decoded signature bytes are what authenticate. Padding variants
   (`=`, `==`), `+`/`/` vs `-`/`_`, and the known final-character slack (86 chars = 516 bits, last
   char carries 4 unused bits) — find any OTHER position with the same slack, or a decoder
   permissiveness that lets two distinct strings decode to the same 64 bytes.
3. **Same for the payload segment.** Two payload strings decoding to the same bytes would let a
   signed payload be re-encoded — try `Base64.getUrlDecoder()`'s tolerance for stray characters.
4. **Algorithm confusion.** `alg: none`, `HS256`, `RS256`, `alg` as an array/object, no `alg`, and a
   `kid`. Expected: all irrelevant, the header is never read. Confirm no code path anywhere reads it.

### B. Break claim validation
5. **Expiry evasion.** `exp` as: a huge int, `1e309`, `-0`, a string of digits, an array, a nested
   object, `NaN`, `Infinity`, a value with 30 significant digits, an integer beyond `Long.MAX_VALUE`
   written without exponent (`99999999999999999999`). Anything that makes `numericDate` return null
   (skip the check) instead of throwing is a **fail-open** and is the highest-value finding here.
6. **Duplicate claims.** `{"exp":<past>,"exp":<future>}` — which wins in kotlinx? Does the winner
   differ between `validateClaims` and `JwtPayload.getClaim`? A split decision between validator and
   reader is a bypass.
7. **`iss`/`aud` confusion.** `aud` as a deeply nested array, an array containing an object that
   *stringifies* to the audience, unicode normalization / homoglyphs of the configured issuer,
   trailing whitespace, case variants. `iss` as a JSON number that renders like the issuer.
8. **`nbf`/`iat` used to widen, not narrow.** A token with `iat` far in the past but `exp` absent —
   confirmed valid forever by design (preserved behaviour); assess whether any deployment relies on
   `exp` being mandatory. Also `nbf` fractional just under `now`.
9. **Clock trust.** `JwtGenerator` takes an injectable `Clock`. Verify nothing production-side can
   supply one (kontainer binding at `funktor/rest/src/jvmMain/kotlin/index_jvm.kt:112` uses the
   default) — a caller-controlled clock is a total expiry bypass.

### C. Identity and permission escalation past a valid signature
Assume an attacker who holds a **legitimately issued** token for a low-privilege user.
10. **Claim shadowing.** Add `permissions/superuser` as the string `"true"`, as `1`, as `["true"]`;
    add a second `user/id`; put `user/id` at a different nesting depth. `extract.kt` degrades rather
    than throws — verify degradation always lands on LESS privilege, never more.
11. **Namespace collision.** Configured `userNs`/`permissionsNs` are string prefixes joined with
    `/`. Craft a claim whose name spans the boundary (`"user/id/../permissions/superuser"`, or a
    `userNs` value containing `/`) so one namespace's claim is read as the other's.
12. **`sub` vs `user/id` disagreement.** `extractUser` prefers `user/id` then falls back to `sub`.
    Send a valid token where they name different users; confirm which is authoritative and that
    logging/audit records the same one authorization used.
13. **Anonymous-with-permissions.** The degraded principal is supposed to drop permissions with
    identity (`ExtractUserSpec` pins it). Attempt an id that is *just* invalid enough to degrade
    identity while some other path still honours the token's roles.

### D. Resource exhaustion pre-authentication
14. **Under the cap, over the budget.** `MAX_TOKEN_LENGTH` is 64 KB. Measure server cost of
    thousands of concurrent 64 KB bearer headers — the HMAC is cheap, but ktor header parsing and
    `String` allocation upstream of the gate are not. Compare against a 64 KB body POST.
15. **Post-authentication payload bombs.** A key holder can send a 60 KB deeply-nested payload that
    passes the MAC. `decodeClaims` then parses it with kotlinx. Establish kotlinx's nesting/size
    behaviour (Jackson capped nesting at 1000 by default; does kotlinx?) and whether a valid token
    can stack-overflow the parser. Lower severity (needs the key) but it is a new surface.
16. **Signature-check cost.** Confirm the length cap is checked before the MAC (it is, by reading)
    and that no logging of the raw token happens on the reject path.

### E. Cross-tenant / cross-issuer
17. **Token from another deployment.** Two apps sharing a signing key but differing in
    `issuer`/`audience` — confirm claim validation is what separates them and that neither accepts
    the other's tokens. Then the inverse: same issuer/audience, different key.
18. **Replay.** Tokens carry no `jti` and there is no revocation (`SessionJwtClaims` is NOT WIRED —
    `.claude/tasks/20260728-session-revocation-wiring.md`). Confirm the practical window: a stolen
    token is valid until `exp`, and a token minted without `exp` is valid forever. Quantify it.

## Known-and-accepted (do not report as new)

- A valid-MAC token whose header names a different algorithm verifies — we never read the header.
  java-jwt rejected it; the divergence is deliberate and only reachable by the key holder.
- `MessageDigest.isEqual`→`==` is not test-observable; constant-time rests on review.
- Missing `exp` means no expiry check — preserved from java-jwt on purpose. Tightening it is a
  policy decision, tracked separately, not a defect of this change.

## Out of scope here

Google SSO's Gson parse of unauthenticated JSON (`google-api-client`, reachable via `AuthApi`'s
`public()` floor). Verify-then-parse cannot apply — RS256 needs `kid` before key selection. Its own
task; maintainer decision pending.
