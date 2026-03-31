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

### C1: CSRF token validation uses non-constant-time string comparison — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:50`
- **Category:** Security
- **Status:** FIXED — Now uses `MessageDigest.isEqual()` for constant-time comparison.

---

## HIGH

### H1: CSRF signing concatenates fields without delimiters — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:53`
- **Category:** Security
- **Status:** FIXED — Now uses null byte (`\u0000`) separators between fields.

### H2: JwtConfig signing key is @Serializable — can leak to logs/JSON — ✅ FIXED

- **File:** `ultra/security/src/commonMain/kotlin/jwt/JwtConfig.kt:8`
- **Category:** Security
- **Status:** FIXED — `toString()` overridden to redact `signingKey`. `@Serializable` kept for config deserialization.

### H3: UltraSecurityConfig allows empty CSRF secret by default — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:12-14`
- **Category:** Security
- **Status:** FIXED — `StatelessCsrfProtection` now validates `require(csrfSecret.isNotBlank())`.

### H4: Password hash serialization format vulnerable to delimiter injection — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/password/PasswordHasher.kt:26-38`
- **Category:** Logic
- **Status:** FIXED — Field count validated; uses `getOrNull` for safe access.

---

## MEDIUM

### M1: CSRF TTL type is Int, limiting max token lifetime — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:8`
- **Category:** Logic
- **Status:** FIXED — Changed `csrfTtlMillis` to `Long` in both `UltraSecurityConfig` and `StatelessCsrfProtection`.

### M2: CSRF signature uses plain SHA-384, not HMAC — DEFERRED

- **File:** `ultra/security/src/jvmMain/kotlin/csrf/StatelessCsrfProtection.kt:53`
- **Category:** Security
- **Status:** DEFERRED — SHA-384 (truncated SHA-512) is not vulnerable to length-extension attacks.
  Switching to HMAC would break existing tokens. Risk is mitigated.

### M3: `JwtGenerator.config` is public, exposing signing key — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:14`
- **Category:** API Design
- **Status:** FIXED — Changed to `internal`. Added `verify(token)` convenience method.

### M4: `JwtGenerator.verifier` is public — ✅ PARTIALLY FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:19`
- **Category:** API Design
- **Status:** PARTIALLY FIXED — Added `verify(token)` method. `verifier` stays public for Ktor JWT plugin integration.

### M5: `CompoundPasswordHasher` exposes `primary` and `hashers` as public — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/password/CompoundPasswordHasher.kt:33,37`
- **Category:** Implementation
- **Status:** FIXED — Changed to `internal`.

### M6: Wildcard imports in JWT files — ✅ FIXED

- **File:** `jwt/JwtNullClaim.kt:5`, `builder.kt:6`, `JwtAnonymous.kt:5`
- **Category:** Code Style
- **Status:** FIXED — All JWT files now use explicit imports.

### M7: UltraSecurityConfig TODO for @JsonIgnore still open — ✅ FIXED

- **File:** `ultra/security/src/jvmMain/kotlin/UltraSecurityConfig.kt:5`
- **Category:** Implementation
- **Status:** FIXED — `toString()` overridden to redact `csrfSecret`. TODO removed.

---

## LOW

### L1: Test uses `setOf("o1, o2")` instead of `setOf("o1", "o2")` — ✅ FIXED

- **File:** `ultra/security/src/jvmTest/kotlin/jwt/JwtPermissionsRoundTripSpec.kt:21-25`
- **Category:** Code Style
- **Status:** FIXED — Both test cases now use properly separated set elements.

### L2: Wildcard imports in JWT test files — ✅ FIXED

- **Category:** Code Style
- **Status:** FIXED — All test files use explicit imports.

### L3: JwtGenerator default expiry (60 min) can't be overridden for issuer/audience/subject — BY DESIGN

- **File:** `ultra/security/src/jvmMain/kotlin/jwt/JwtGenerator.kt:32-39`
- **Category:** API Design
- **Status:** BY DESIGN — Expiry IS overridable (builder runs after default). Issuer/audience/subject are
  intentionally non-overridable security properties.

---

## Test Coverage Gaps

No tests for: `JwtAnonymous`, `JwtNullClaim`, `UserProvider.Lazy` thread safety, CSRF with empty/blank secrets,
`UserPermissions` branch/group/role/permission variants (only organisation tested).
