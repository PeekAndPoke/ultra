# JWT library consolidation — options (backlog / not scheduled)

**Status:** REOPENED 2026-07-31 on new evidence — the 2026-07-18 "keep java-jwt" recommendation was made before the Jackson removal and does not account for pre-verification parsing. See the new section at the bottom.
**Type:** reference / backlog

## Current state (keep for now)

- **Server (JVM):** `com.auth0:java-jwt:4.5.1` — signs + cryptographically verifies JWTs
  (`ultra/security` `JwtGenerator`, `funktor/rest` `Caller`, `funktor/auth` `SessionJwtClaims`).
  Industry-standard, mature. **Keep.**
- **Client (JS):** `npm("jwt-decode", "3.1.2")` via the kraft `jwtdecode` addon — decode-only
  (base64url payload → JSON, no signature verification). Injected into `AuthState.jwtDecoder`.
  There's a `TODO: check update to 4.x` on it. **Keep at 3.1.2 for now — do NOT bump.**

## Why not unify onto one KMP JWT lib

- **No official JetBrains KMP JWT library exists** (Ktor's JWT support is JVM-only).
- The server is JVM-only, so multiplatform buys nothing on the sign/verify path; moving
  security-critical verification off battle-tested java-jwt to a younger lib is a bad trade.
- The only genuinely multiplatform need is the **client-side decode**, which needs no JWT lib at
  all — just base64url + JSON (both stdlib).

## Options for later

1. **(Recommended, low-risk) Client: drop `jwt-decode`, decode in pure Kotlin `commonMain`.**
   `kotlin.io.encoding.Base64.UrlSafe` (padding ABSENT_OPTIONAL) + `kotlinx.serialization.json`.
   ~15 lines, zero deps, truly multiplatform, kills the npm/ESM churn + the 4.x TODO. Also fixes
   the demo bug where `AuthState.jwtDecoder` is never wired (client permissions currently empty).
   Server stays on java-jwt. **This is the one to do eventually.**
2. **Full KMP unification** onto one lib (candidates below). Only worth it if we ever need to
   sign/verify OFF the JVM (native client verifying locally, offline apps). Not the case today.
   - [Appstractive/jwt-kt](https://github.com/Appstractive/jwt-kt) — KMP create/parse/sign/verify
     (HMAC/RSA/ECDSA), built on community `cryptography-kotlin`. Most complete.
   - [JWT-Kotlin / jwt-kmp](https://klibs.io/project/KotlinMania/JWT-Kotlin) — auth0-API-compatible
     HMAC port (`io.github.kotlinmania:jwt-kmp`).
   - [KotJWT](https://github.com/iNoles/KotJWT) — KMP encode/decode/verify.
   - Decode-only: [KMP-Simple-JWT-Parser](https://github.com/jmseb3/KMP-Simple-JWT-Parser).
   Trade-off: gives up java-jwt's maturity on the security-critical server path.
3. **Status quo** — keep both libs. Current choice.

## If we ever bump jwt-decode to 4.x

Breaking change is only the export shape: default → named (`import { jwtDecode }`). The kraft addon's
"call the module directly" usage (`jwtDecodeModule(jwt)`) would need `.jwtDecode`. About the same
effort as deleting the dep → prefer option 1 over bumping.


---

# REOPENED 2026-07-31 — the parser runs BEFORE the signature is checked

The original recommendation above ("keep java-jwt, it is battle-tested") weighed the *JWT logic*. It did
not weigh what the library drags in, or when that code runs. Raised by the maintainer while removing
Jackson: **a JWT's JSON is parsed before anything is verified, so an unauthenticated attacker has a
direct line to the parser.**

## Confirmed by experiment, not by reading

Probed against the real `java-jwt` verifier with `Algorithm.HMAC512` and a deliberately invalid
signature:

| Token | Result |
|---|---|
| valid JSON, bad signature | `SignatureVerificationException` — the baseline |
| **malformed payload**, bad signature | `JWTDecodeException: ... doesn't have a valid JSON format` |
| **malformed header**, bad signature | `JWTDecodeException: ... doesn't have a valid JSON format` |
| 10 000-deep nested array payload | `JWTDecodeException` — caught by Jackson's own nesting limit |
| **20 MB string payload**, bad signature | `SignatureVerificationException` |

A *parse* error, not a signature error, means parsing ran first. The last row is the sharpest: the 20 MB
string was **fully parsed and allocated**, and only then did the signature check fail. That is a
pre-authentication resource attack against any endpoint that accepts a bearer token.

## Swapping libraries does NOT remove this

Every general-purpose JWT library must parse before verifying, because the header is what tells it which
algorithm to verify with. The risk class is structural, not a java-jwt defect.

| Library | JSON engine | Note |
|---|---|---|
| `com.auth0:java-jwt` (current) | **Jackson**, unshaded | Puts Jackson on our classpath — the one thing the rest of the codebase just removed |
| `com.nimbusds:nimbus-jose-jwt` | **shaded Gson** since 9.24 (was json-smart) | Shading avoids classpath conflict, but **CVE-2025-53864**: DoS via a deeply nested JSON object in a JWT claim set, uncontrolled recursion, before 10.0.2. Exactly this attack class, on the main alternative, last year |
| `io.jsonwebtoken:jjwt` 0.13.0 | **pluggable** — `jjwt-gson` avoids Jackson entirely | The best library answer: the JSON backend is a runtime dependency you choose |

## The option no library can offer us: VERIFY, THEN PARSE

This codebase signs and verifies with **one fixed symmetric algorithm** — `HMAC512`, single key, no
negotiation (`JwtGenerator.kt:18`). Nothing ever needs to read `alg` to decide anything.

That permits an order no general-purpose library can use:

1. Split the token on `.` — pure string handling, no parsing.
2. HMAC-SHA512 the `header.payload` bytes and compare, constant-time, against the signature segment.
3. **Only then** base64url-decode and hand the payload to kotlinx.

The parser then only ever sees bytes we have already authenticated. This does not *mitigate* the
pre-auth parser surface — it **removes** it. A 20 MB or deeply-nested payload is rejected by an HMAC
over raw bytes, which allocates nothing beyond the token itself.

**What rolling our own would and would not cost:**

- ✅ **alg confusion** (RS256↔HS256) — not applicable, `alg` is never read.
- ✅ **`alg: none`** — not applicable, same reason.
- ⚠️ **Constant-time comparison** — must use `MessageDigest.isEqual`, not `==`.
- ⚠️ **`exp` / `nbf` / `iat` / `iss` / `aud`** — ours to implement and test. This is where a hand-rolled
  JWT usually goes wrong, and it is the part worth the most test effort.
- ⚠️ Token size cap before any work — trivial, and something the library does not give us today.

The production surface to replace is small: `JwtGenerator` (create/sign, require/verify), `builder.kt`
(claim setters) and `extract.kt` (claim getters). All JVM-only, all in `ultra/security`.

**This is a different proposition from the 2026-07-18 objection.** That objection was against adopting a
*younger general-purpose library*, and it still stands. ~100 lines implementing one algorithm, with the
verify-then-parse inversion, is not that — and it is the only option that closes the hole rather than
relocating it.

## Recommendation

1. **If a library is preferred:** `jjwt` with `jjwt-gson`. Removes Jackson, keeps a maintained
   implementation. Does **not** fix verify-after-parse.
2. **If the pre-auth parser surface is the actual concern** — which is how it was raised — only
   verify-then-parse addresses it, and only because we use one symmetric algorithm.

Either way the client-side `jwt-decode` npm dependency should go (option 1 in the original list above);
that is unchanged and independent.

**Not scheduled. Security-critical: yes — this is the pre-authentication path.**
