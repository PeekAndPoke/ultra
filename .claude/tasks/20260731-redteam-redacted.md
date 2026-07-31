# RED TEAM — `Redacted<T>`, the one-way secret wrapper

**Status:** COLLECTED — do not execute here. For a dedicated penetration-test session.
**Created:** 2026-07-31
**Plan:** `.claude/tasks-archive/2026-07/20260731-redacted-and-jackson-removal.md`
**Why:** `Redacted<T>` is now the *single* mechanism keeping secrets out of serialized output — the
signing key, the CSRF secret, and both database passwords. `ConfigRedaction` was deleted, so there is
no second net. Its own KDoc says it plainly: *"The type alone protects nothing. Each serializer must be
taught about it."*

## The code under attack

| File | Role |
|---|---|
| `ultra/common/src/commonMain/kotlin/model/Redacted.kt` | the type, its kotlinx serializer, `toString`/`equals`/`hashCode` |
| `ultra/slumber/src/jvmMain/kotlin/builtin/model/RedactedCodec.kt` | the Slumber awaker/slumberer |
| `ultra/slumber/src/jvmMain/kotlin/builtin/BuiltInModule.kt` | branch order — Redacted must be matched FIRST |
| `funktor/insights/…/AppConfigCollector` | the collector that leaked all four secrets before this existed |

## Scenarios to attempt

### A. Get the secret out through a serializer that was never taught
1. **Enumerate every serializer in the codebase and confirm each redacts.** kotlinx and Slumber are
   handled. What about: Java serialization (`ObjectOutputStream` — `Redacted` is not `Serializable`,
   confirm that *fails* rather than silently succeeding), any logging framework that reflects over
   objects (logback structured appenders, `ultra/log`'s own encoders), ktor's error/debug rendering,
   the AQL and Mongo printers, and the TS SDK codegen path. **Adding a serializer is the documented way
   this breaks** — so the attack is "find one nobody taught".
2. **Reflection.** `value` is a public property. Anything walking properties reflectively — a debug
   dumper, an insights collector added later, `ultra/reflection` — will find it. Find a reachable one.
3. **Slumber branch order.** `RedactedCodec` is registered as the FIRST branch in `getAwaker` and
   `getSlumberer`. Try to reach a path where an earlier or more specific branch claims the type first
   (nested generics, `Redacted` inside a collection/map, `Redacted<Redacted<String>>`, a `Redacted` in a
   polymorphic/sealed position).
4. **`Redacted<T>` where T is a collection or a data class.** The subtree case is the selling point;
   verify a `Redacted<AwsConfig>` really emits a placeholder rather than descending.

### B. Turn the broken round trip into a live outage or a known key
5. **The restore path — highest value here.** By design, serializing a `Redacted<String>` yields
   `"***redacted***"`, and reading that back yields a `Redacted<String>` holding the literal placeholder.
   So *any* path that round-trips config through a serializer silently replaces the signing key with a
   **publicly known constant**, and the app would then mint tokens anyone can forge. Hunt for such a
   path: insights record → restore, config snapshot → reload, cluster config propagation, a
   copy-config-to-another-env tool, test fixtures that serialize an `AppConfig`. The KDoc says a
   `Redacted<SomeObject>` throws on re-read; the `Redacted<String>` case does NOT throw, which is
   exactly what makes it dangerous.
6. **Startup validation.** If such a path exists, does anything detect that the signing key equals
   `Redacted.PLACEHOLDER` and refuse to boot? Assess whether a boot-time guard is warranted.

### C. Oracles on the wrapper itself
7. **`equals` is a direct guess-checker.** `Redacted(a) == Redacted(b)` compares the *inner* values.
   Find any path where attacker-supplied input is wrapped in `Redacted` and compared against a real
   secret — that is a test-one-guess primitive, and it is not constant-time.
8. **`hashCode` leaks a 32-bit digest of the secret.** `hashCode()` returns `value.hashCode()`. For a
   `String` that is a well-known, cheap, non-cryptographic function — offline-invertible for short or
   low-entropy secrets. Look for anywhere a `Redacted` reaches a `HashMap`/`HashSet` whose iteration
   order or bucket layout is observable, or anywhere a hash is logged or exposed in diagnostics.
   Consider whether `hashCode` should be a constant instead.
9. **`toString` on containers.** `Redacted.toString()` redacts, but confirm the containing config
   classes have no hand-written `toString` that reads `.value`, and that generated `data class`
   `toString` on the *parent* really calls `Redacted.toString()`.

### D. Regression — the four original leaks
10. Re-verify all four secrets that leaked before this existed are now redacted end-to-end, by reading a
    real insights record off disk rather than by unit test: `JwtConfig.signingKey`,
    `UltraSecurityConfig.csrfSecret`, `ArangoDbConfig.password`, `MongoDbConfig.connectionString`.
    Note `connectionString` is a *URL with credentials embedded* — check the whole string is replaced,
    not just a password field.
11. **`AppConfig.keys`** is now `Map<String, Redacted<String>>`. Confirm map KEYS are not themselves
    sensitive (a key named after what it unlocks) and that the map's own serialization does not emit
    entries whose values fell back to generic object handling.

## Known-and-accepted (do not report as new)

- `.value` is public and trivially readable in-process. That is the type's purpose; it defends against
  *accidental serialization*, not against code that deliberately reads it.
- The round trip is broken deliberately. Scenario 5 is about finding a path where that breakage is
  *reachable in production*, not about the breakage itself.

## Out of scope

Secrets that never pass through `Redacted<T>` — environment variables, mounted files, anything a
third-party library holds internally.
