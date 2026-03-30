# Audit: ultra:security — Issues Report

**Date:** 2026-03-30
**Auditor:** Code audit agent (Wave 1)
**Module:** ultra/security (~33 source files, 12 test files)

---

## Summary

| Category       | CRITICAL | HIGH  | MEDIUM | LOW   |
|----------------|----------|-------|--------|-------|
| Logic          | 0        | 1     | 1      | 0     |
| Implementation | 0        | 0     | 1      | 1     |
| Security       | 1        | 3     | 2      | 0     |
| Code Style     | 0        | 0     | 1      | 2     |
| API Design     | 0        | 0     | 2      | 1     |
| **Total**      | **1**    | **4** | **7**  | **4** |

---

## CRITICAL

### C1: CSRF token validation uses non-constant-time string comparison

- **File:** `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:50`
- **Category:** Security
- **Impact:** `expectedSignature == receivedSignature` uses Kotlin's short-circuiting `==`. Attacker can mount timing
  attack to forge CSRF tokens by measuring response times. This is the CSRF protection module — the most
  timing-sensitive comparison.
- **Fix:** Use `java.security.MessageDigest.isEqual(expected.toByteArray(), received.toByteArray())` for constant-time
  comparison, exactly as the PBKDF2 hasher already does.

---

## HIGH

### H1: CSRF signing concatenates fields without delimiters — signature collision

- **File:** `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:53`
- **Category:** Security
- **Impact:** Sign function builds `"$salt$userId$clientIp$ttl$csrfSecret"` with no delimiters. `userId="AB"` +
  `clientIp="CD"` produces same signature as `userId="ABC"` + `clientIp="D"`. Enables cross-user CSRF token reuse under
  specific conditions.
- **Fix:** Use null byte separator: `"$salt\u0000$userId\u0000$clientIp\u0000$ttl\u0000$csrfSecret"`.

### H2: JwtConfig signing key is @Serializable — can leak to logs/JSON

- **File:** `ultra/security/src/commonMain/kotlin/jwt/JwtConfig.kt:8`
- **Category:** Security
- **Impact:** `JwtConfig` is a `@Serializable` data class with `signingKey` as plain String. If serialized (logs, debug
  endpoints, error messages), the HMAC-512 key is exposed. Allows forging any JWT.
- **Fix:** Remove `@Serializable` from `JwtConfig`, or mark `signingKey` with `@Transient`, or override `toString()` to
  redact.

### H3: UltraSecurityConfig allows empty CSRF secret by default

- **File:** `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:12-14`
- **Category:** Security
- **Impact:** `UltraSecurityConfig.empty` sets `csrfSecret = ""`. CSRF tokens become trivially forgeable with no secret
  material. No validation or warning.
- **Fix:** Add `require(csrfSecret.isNotBlank()) { "CSRF secret must not be blank" }` in `StatelessCsrfProtection` init.
  Mark `empty` as test-only.

### H4: Password hash serialization format vulnerable to delimiter injection

- **File:** `ultra/security/src/jvmMain/kotlin/password/PasswordHasher.kt:26-38`
- **Category:** Logic
- **Impact:** `Hash.fromString()` splits on `:` and `asString()` joins with `:`. No escaping or field-count validation.
  If a future hasher produces output containing `:`, password verification silently fails. `fromString` accepts
  malformed input with fewer than 3 parts, defaulting missing fields to `""`.
- **Fix:** Validate `parts.size == 3` and throw on malformed input. Consider joining index 2+ as hash value.

---

## MEDIUM

### M1: CSRF TTL type is Int, limiting max token lifetime

- **File:** `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:8`
- **Category:** Logic
- **Impact:** `csrfTtlMillis` is `Int` (overflows at ~24.8 days). Design smell — should match
  `System.currentTimeMillis()` type.
- **Fix:** Change to `Long`.

### M2: CSRF signature uses plain SHA-384, not HMAC

- **File:** `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:53`
- **Category:** Security
- **Impact:** Uses plain digest with keyed secret instead of HMAC. SHA-384 is not vulnerable to length-extension (
  truncated SHA-512), so mitigated here, but deviates from best practice.
- **Fix:** Replace with `javax.crypto.Mac` using `HmacSHA384`.

### M3: `JwtGenerator.config` is public, exposing signing key

- **File:** `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:14`
- **Category:** API Design
- **Impact:** Any code with JwtGenerator reference can read `config.signingKey`.
- **Fix:** Change to `private` or `internal`.

### M4: `JwtGenerator.verifier` is public

- **File:** `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:19`
- **Category:** API Design
- **Impact:** Allows callers to verify tokens outside generator's control.
- **Fix:** Make `private`, add `verify(token: String)` method.

### M5: `CompoundPasswordHasher` exposes `primary` and `hashers` as public

- **File:** `ultra/security/src/jvmMain/kotlin/password/CompoundPasswordHasher.kt:33,37`
- **Category:** Implementation
- **Impact:** Leaks internal hasher instances. Violates minimal surface area.
- **Fix:** Change to `private` or `internal`.

### M6: Wildcard imports in JWT files

- **File:** `jwt/JwtNullClaim.kt:5`, `builder.kt:6`, `JwtAnonymous.kt:5`
- **Category:** Code Style
- **Impact:** `import java.util.*` — should be explicit.
- **Fix:** Replace with `import java.util.Date`.

### M7: UltraSecurityConfig TODO for @JsonIgnore still open

- **File:** `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:5`
- **Category:** Implementation
- **Impact:** `csrfSecret` can be visible on logs/insights.
- **Fix:** Resolve before v1 — add serialization exclusion or redact in `toString()`.

---

## LOW

### L1: Test uses `setOf("o1, o2")` instead of `setOf("o1", "o2")`

- **File:** `ultra/security/src/jvmTest/kotlin/jwt/JwtPermissionsRoundTripSpec.kt:21-25`
- **Category:** Code Style
- **Impact:** Creates single-element set, never tests multi-element permission sets. Weakens test coverage.
- **Fix:** Change to `setOf("o1", "o2")`.

### L2: Wildcard imports in JWT test files

- **Category:** Code Style
- **Fix:** Replace with explicit imports.

### L3: JwtGenerator default expiry (60 min) can't be overridden for issuer/audience/subject

- **File:** `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:32-39`
- **Category:** API Design
- **Impact:** Builder lambda runs before issuer/audience/subject are set. Builder cannot override those. Documented but
  potentially surprising.

---

## Test Coverage Gaps

No tests for: `JwtAnonymous`, `JwtNullClaim`, `UserProvider.Lazy` thread safety, CSRF with empty/blank secrets,
`UserPermissions` branch/group/role/permission variants (only organisation tested).
