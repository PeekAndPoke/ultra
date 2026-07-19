# Environment classification must fail closed on unknown environments

**Status:** TODO
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (finding 3)
**Security-critical:** yes

## Spec

`funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt:42-44` defines production as an
allow-list and derives everything else by negation:

```kotlin
val isProduction: Boolean get() = deployment.environment.lowercase() in listOf("live", "prod", "production")
val isNotProduction: Boolean get() = !isProduction
```

Any environment string outside that list — `staging`, `prd`, `production-eu`, `live-de`, a trailing
space, or an unresolved `${ENV}` from a templating bug — is classified non-production, so every
`isNotProduction` read takes the permissive branch. The permissive branches serve full stack traces
(`ApiStatusPages`) and failed-auth-rule descriptions (`respond.kt:57-66`).

Make the *permissive* side the allow-list so an unrecognised environment is treated as production.

- [ ] `isNotProduction` is derived from its own allow-list (`dev`, `test`, `local`, `qa*`), not from
      `!isProduction`
- [ ] An unrecognised environment string yields `isProduction == true` **and**
      `isNotProduction == false` (both restrictive — they need not be strict complements)
- [ ] `staging`, `prd`, `preprod`, `production-eu`, `""`, `"${ENV}"` all classify as production
- [ ] Preferably: `environment` is parsed into a sealed type at config load and startup **fails**
      on an unrecognised value, so misconfiguration is loud rather than silently permissive
- [ ] Decide and document whether `staging` should be production-like (recommended: yes for
      disclosure purposes)

## Implementation notes

Root cause shared with:
- `.claude/tasks/20260720-fixtures-env-gate.md` (finding 4) — same inversion, different module
- finding 5 in the audit doc (`funktor/rest/src/jvmMain/kotlin/respond.kt:57-66`), which is fixed
  for free by this change

**BREAKING CHANGE FOR EXISTING TESTS.** Two specs currently assert today's permissive behaviour and
were written before this finding existed. They encode current semantics, not a desired invariant,
and **must be inverted** as part of this task:

- `funktor/core/src/jvmTest/kotlin/config/ktor/KtorConfigSpec.kt` — "environments must not be
  classified as production by prefix or substring" asserts `preprod`/`prod-clone`/`production-mirror`
  are non-production; and `staging` is asserted non-production
- `funktor/rest/src/jvmTest/kotlin/ApiStatusPagesSpec.kt` — "environments that merely look like
  production must not be treated as production" asserts they *may* expose stack traces

Also consider `respond.kt:57-66`: `appConfig` there throws when no kontainer is on the call, turning
a clean 401 into a 500. Switching it to `kontainerOrNull?.getOrNull(AppConfig::class)?.ktor
?.isNotProduction == true` mirrors the `ApiStatusPages` fix and removes that edge.

## Test evidence

- [ ] Unit tests for every classification helper against: the three production names, casing
      variants, the dev/test/qa names, and a table of unknown strings
- [ ] Inverted assertions in the two specs listed above
- [ ] End-to-end: not required (pure config logic, no storage)
- [ ] Full test command(s) run + green: `./gradlew :funktor:core:jvmTest :funktor:rest:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
