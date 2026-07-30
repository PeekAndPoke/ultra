# JWT library consolidation — options (backlog / not scheduled)

**Status:** COLLECTED — decision deferred. Keep current libs as-is for now (no version bump).
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
