# RED TEAM — our own JWT verifier (post java-jwt removal)

**Status:** COLLECTED — do not execute here. For a dedicated penetration-test session.
**Created:** 2026-07-31
**Plan:** `.claude/tasks/20260718-jwt-lib-consolidation-options.md` (RESOLVED section)
**Why:** `com.auth0:java-jwt` was removed on 2026-07-31 and this codebase now owns JWT signing,
signature verification and registered-claim validation outright. Every request bearing an
`Authorization: Bearer` header hits this code, unauthenticated.

**Updated 2026-07-31 (commit `9fe2a21a`)** — the single signing key became a `kid`-selected key
LIST. Two things changed that matter to an attacker, and section F is new because of them:

- The **header is now parsed before the MAC**, because the key cannot be selected without it. It is
  length-capped (`MAX_HEADER_LENGTH`, on the ENCODED segment, before base64-decoding) and parsed
  into a fixed three-field `JwtHeader`, not a `JsonObject`. The payload is still never parsed
  pre-auth. This is a NEW pre-authentication parser surface — attack it.
- `kid` is **attacker-controlled input used to select a key**. It is meant to be nothing but a map
  lookup. Prove that.

## The code under attack

| File | Role |
|---|---|
| `ultra/security/src/jvmMain/kotlin/jwt/JwtSignatureGate.kt` | length caps, shape check, header parse, `kid` lookup, `alg` comparison, HMAC over raw segments, constant-time compare |
| `ultra/security/src/jvmMain/kotlin/jwt/JwtHeader.kt` | the ONE structure parsed from unauthenticated input |
| `ultra/security/src/commonMain/kotlin/jwt/JwtSigningKey.kt` | key object: `id` (the `kid`), `secret`, `alg`, `issued` |
| `ultra/security/src/commonMain/kotlin/jwt/JwtAlgorithm.kt` | header value ↔ JCA name ↔ min key bytes |
| `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt` | `verify` order, payload decode, `validateClaims`, `sign` |
| `ultra/security/src/jvmMain/kotlin/jwt/JwtBuilder.kt` | claim assembly / wire shapes |
| `ultra/security/src/jvmMain/kotlin/jwt/extract.kt` | claims → `UserId` / `UserPermissions` |
| `funktor/rest/src/jvmMain/kotlin/auth/jwtCaller.kt` | the ktor entry point (`tryJwtCaller`) |

## Scenarios to attempt

### A. Forge a token without the key
1. **Signature bypass by shape.** Tokens with extra dots, trailing dots, empty segments, a signature
   segment that base64url-decodes to zero bytes, and unicode/whitespace-padded segments. The gate
   now pins the token at **exactly two dots and three non-empty segments** using
   `indexOf`/`lastIndexOf` arithmetic (`JwtSignatureGate.check`), then `substring`s the signing
   input. Attack that arithmetic: make the bytes the MAC covered and the bytes `decodeClaims` reads
   disagree. Surrogate pairs and any character where `String.length` (UTF-16 units) differs from the
   byte length are the interesting inputs, because the caps are in `length` and the MAC is over
   UTF-8 bytes.
2. **Base64url malleability.** The decoded signature bytes are what authenticate. Padding variants
   (`=`, `==`), `+`/`/` vs `-`/`_`, and the known final-character slack (86 chars = 516 bits, last
   char carries 4 unused bits) — find any OTHER position with the same slack, or a decoder
   permissiveness that lets two distinct strings decode to the same 64 bytes.
3. **Same for the payload segment.** Two payload strings decoding to the same bytes would let a
   signed payload be re-encoded — try `Base64.getUrlDecoder()`'s tolerance for stray characters.
4. **Algorithm confusion.** The rule is now: the KEY's `alg` is authoritative and the header's is
   only COMPARED (`JwtAlgorithm.byHeaderValue(header.alg) != entry.key.alg` rejects). Attack the
   comparison, not the selection: `alg` as an array/object/number, absent, `"HS512 "` with trailing
   whitespace, `"hs512"`, unicode-normalisation variants, a duplicate `alg` member where kotlinx's
   winner differs from what a reader would expect. **The finding that matters is any input where the
   comparison passes but the MAC then runs under an algorithm the key did not authorise** — or any
   path that reaches `Mac.getInstance` with a name derived from the header rather than the key.

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

### F. `kid` selection and key rotation  *(new — commit `9fe2a21a`)*
19. **`kid` as anything but a map key.** It is attacker-controlled and reaches `prepared[header.kid]`.
    Prove it never becomes a path, a query fragment, a fetch, a cache key, a log line or a metric
    label — now or after the next refactor. Try: `../../etc/passwd`, a 60 KB kid (bounded only by
    `MAX_HEADER_LENGTH`), NUL and control characters, newlines (log injection into anything that
    later records a rejection), `%00`, and a kid that is valid JSON-escaped but decodes to something
    else. **Also check what `kid` reaches on the SUCCESS path** — a verified request's key id is
    exactly the sort of thing that gets added to an insights record or an access log later.
20. **Key-enumeration oracle.** An unknown `kid` rejects before the MAC; a known `kid` with a bad
    signature rejects after it. The messages are identical (asserted by a spec) but the TIMING is
    not, by roughly one HMAC. Measure whether that is distinguishable over a network, and decide
    whether learning the set of configured `kid`s is worth anything — the current judgement is that
    it is not, since every issued token carries one in the clear. **Overturn that if you can**: a
    deployment where kid names leak rotation cadence, environment, or a key's age is the interesting
    case.
21. **Rotation races.** A token minted under key A, verified while A is still configured, replayed
    after A is dropped, and again after a NEW key is added under the id `A` with different material.
    The last is the sharp one: reusing a retired `kid` for fresh material silently makes old tokens
    fail closed — confirm it is *closed* and not merely different, and that nothing caches a
    previously successful verification by `kid`.
22. **Downgrade between configured keys.** With several keys configured, can a token be made to
    verify under a WEAKER one than the issuer intended — a key with a shorter secret that still
    clears its algorithm's floor, or (once a second `JwtAlgorithm` entry exists) a weaker algorithm?
    Today there is only HS512; the attack becomes live the moment a second entry is added, so record
    the shape now.
23. **Header parser abuse (pre-authentication).** `MAX_HEADER_LENGTH` (1024) bounds the ENCODED
    segment, so the parser sees at most ~768 bytes. Within that budget: deeply nested JSON in an
    ignored member, huge numbers, `\u` escape bombs, duplicate `kid`/`alg` members, invalid UTF-8
    byte sequences after base64-decoding, a BOM, and JSON that is valid but pathological for
    kotlinx. Establish the real worst-case CPU and allocation for 768 bytes of attacker JSON, and
    compare it to the HMAC it replaced as the first cost an unauthenticated caller can impose.
24. **Boot validation gaps.** `requireUsableKeys` checks: non-empty, unique ids, non-blank id, secret
    length vs the algorithm's floor. What misconfiguration still boots? Duplicate SECRETS under
    different ids, a key whose secret is the `Redacted` placeholder, a 64-byte secret of one repeated
    character, a kid with a newline, `keys` containing the same object twice. Decide which of those
    deserve a boot check and which are the operator's problem.

## Known-and-accepted (do not report as new)

- **CLOSED, do not report:** a valid-MAC token whose header named a different algorithm used to
  verify, because the header was never read. Since `9fe2a21a` the header's `alg` is compared against
  the key's and a mismatch rejects. The old divergence from java-jwt is gone.
- `MessageDigest.isEqual`→`==` is not test-observable; constant-time rests on review. This is the one
  guard that survived mutation testing, knowingly.
- An unknown `kid` and a bad MAC are distinguishable by TIMING (see 20). Accepted deliberately: `kid`
  values ride in every issued token, so they are not secret. Report only if you can show a
  deployment where the kid set itself is sensitive.
- Missing `exp` means no expiry check — preserved from java-jwt on purpose. Tightening it is a
  policy decision, tracked separately, not a defect of this change.
- No `typ` check (RFC 8725 §3.11). Rests on an unenforced invariant: these keys sign exactly ONE kind
  of JWT. Report the *invariant being broken*, not the missing check.
- Every token issued before `9fe2a21a` stops verifying — `kid` is required and there is no fallback.
  That is the intended one-time logout, not a defect.

## Out of scope here

Google SSO's Gson parse of unauthenticated JSON (`google-api-client`, reachable via `AuthApi`'s
`public()` floor). Verify-then-parse cannot apply — RS256 needs `kid` before key selection. Its own
task; maintainer decision pending.
