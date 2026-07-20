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
(`ApiStatusPages`) and failed-auth-rule descriptions (`respond.kt:57`), and — the worst instance —
register live fixture-installation machinery (`fixtures_module.kt:16`).

Make the *permissive* side the allow-list so an unrecognised environment is treated as production.

- [ ] `isNotProduction` is derived from its own allow-list (`dev`, `test`, `qa*`), not from
      `!isProduction` — see item 4 below; recommended shape reuses the existing `isDevelopment`
      allow-list rather than inventing a second one (open question: the phrase "`local`" in an
      earlier draft of this checklist does not match any code — `isLocalDev` only recognises the
      literal string `"dev"`, there is no `"local"` value anywhere in the codebase; treat that as a
      stale reference, not a new requirement, unless a reviewer says otherwise)
- [ ] An unrecognised environment string yields `isProduction == true` **and**
      `isNotProduction == false` (both restrictive — they need not be strict complements of a
      *second* allow-list, but they should both be derived from the one trusted allow-list,
      `isDevelopment`)
- [ ] `staging`, `prd`, `preprod`, `production-eu`, `""`, `"${ENV}"` all classify as production
- [ ] Preferably: `environment` is parsed into a sealed type at config load and startup **fails**
      on an unrecognised value, so misconfiguration is loud rather than silently permissive
      (tracked as a stretch goal in item 4 below — larger behavior change, needs explicit sign-off
      before implementing, see Implementation notes)
- [ ] Decide and document whether `staging` should be production-like (recommended: yes for
      disclosure purposes — see item 5 below; falls out for free under the recommended design,
      no special-case needed)

## Analysis

All line numbers below were read directly from the files cited; nothing here is inferred from the
audit doc alone. Where something could not be confirmed from source, it is marked **OPEN**.

### 1. Current classification helpers, exactly as written

`funktor/core/src/jvmMain/kotlin/config/ktor/KtorConfig.kt`:

- `deployment.environment` defaults to `"prod"` — `KtorConfig.kt:12` (`Deployment(val environment: String = "prod", ...)`).
- `isLocalDev` — `KtorConfig.kt:34` — `deployment.environment.lowercase() == "dev"` (exact match, only `"dev"`).
- `isTest` — `KtorConfig.kt:36` — `deployment.environment.lowercase() == "test"` (exact match, only `"test"`).
- `isQa` — `KtorConfig.kt:38` — `deployment.environment.lowercase().startsWith("qa")` (prefix match — this is the one helper already written as an allow-list rather than a negation).
- `isDevelopment` — `KtorConfig.kt:40` — `isLocalDev || isTest || isQa`. This is itself a sound
  allow-list: unknown strings fall through to `false`. It is the *only* one of the six helpers that
  already fails closed.
- `isProduction` — `KtorConfig.kt:42` — allow-list of exactly `"live"`, `"prod"`, `"production"`.
- `isNotProduction` — `KtorConfig.kt:44` — `!isProduction`, i.e. derived by negating the
  allow-list above, which is what makes it fail open.

Note the default value itself is currently safe by *coincidence*: `Deployment()` defaults to
`environment = "prod"` (`KtorConfig.kt:12`), and `AppConfig.empty` resolves to `NullAppConfig`
(`funktor/core/src/jvmMain/kotlin/config/AppConfig.kt:145-149`) which uses a bare `KtorConfig()` —
so the *no-config-at-all* case is `isProduction == true` today. The bug is specifically about an
**explicit but unrecognised** string, not about missing configuration.

### 2. Every read of the six flags, repo-wide (blast-radius table)

Searched `**/*.kt` across the whole repo (all modules — funktor, funktor-demo, karango, kraft,
ultra, monko, mutator — excluding `build/` output) for `isLocalDev|isTest|isQa|isDevelopment|isProduction|isNotProduction`. Complete list of non-test, non-definition call sites:

| Site | Flag read | What the permissive/negative branch does | Unknown-env outcome |
|---|---|---|---|
| `funktor/core/src/jvmMain/kotlin/fixtures/fixtures_module.kt:16` | `isProduction` | `else` branch (i.e. `isProduction == false`) registers `SimpleFixtureInstaller` plus `InstallFixturesCliCommand` and `ListFixturesCliCommand` (`fixtures_module.kt:18-25`) | **FAIL OPEN**, and the worst instance: an unrecognised environment gets live, CLI-triggerable fixture installation — potentially data-destructive, not just a disclosure. Already tracked separately in `.claude/tasks/20260720-fixtures-env-gate.md`, which depends on this task fixing the flags it consults. |
| `funktor/messaging/src/jvmMain/kotlin/senders/senders.kt:13` | `isTest` (exact-match allow-list, not negation) | `devConfig.disableEmails \|\| config.ktor.isTest` picks `NullEmailSender`; otherwise the real sender is used (`senders.kt:13-17`) | **FAIL OPEN** for real-send, but scoped: `applyDevConfig` is only invoked with a non-null `devConfig` from `config.devOverrides?.mailing` (`funktor-demo/server/src/main/kotlin/kontainer.kt:85`) — i.e. only when a config file explicitly opts into dev overrides. Risk case: a `staging` config cloned from `dev.conf` that keeps `devOverrides.mailing` but sets `environment = "staging"` would send real emails instead of nulling them, because `isTest` requires the literal string `"test"`. |
| `funktor-demo/server/src/main/kotlin/server.kt:25` | `isTest` | `if (!config.ktor.isTest) { launchWorkers { ... } }` (`server.kt:25-30`) | Inverted risk profile: unrecognised/misspelled env → `isTest == false` → workers **do** launch. Not a disclosure issue (production is supposed to run workers), but an operational one — a genuine test environment with a typo'd `environment` value silently gets a full worker fleet instead of being suppressed. Worth a defensive test but lower severity than the fixtures/disclosure findings. |
| `funktor/rest/src/jvmMain/kotlin/respond.kt:57` | `isNotProduction` (via non-null `appConfig`, which throws if no kontainer — `core_module.kt:55`) | `true` branch appends `"Failed auth rules: " + failedRules.joinToString(...)` to the 401 body (`respond.kt:57-65`) | **FAIL OPEN** (info disclosure): unrecognised env → `isProduction == false` → `isNotProduction == true` → auth-rule internals leak in every 401. |
| `funktor/rest/src/jvmMain/kotlin/ApiStatusPages.kt:32-34` (`exposesStackTraces`), consumed at `ApiStatusPages.kt:50` | `isNotProduction`, guarded by a **null-safe** chain: `config?.ktor?.isNotProduction == true` | `true` → `withCause` attaches `cause.stackTraceToString()` to the error response (`ApiStatusPages.kt:47-53`) | **FAIL OPEN** for a non-null config carrying an unrecognised string. The null-config path already fails closed today (`config == null` → `exposesStackTraces` returns `false`, confirmed by `ApiStatusPagesSpec.kt:17-21` "an unknown environment must be treated as production") — that part of the finding is already fixed. This task only needs to fix the case where `AppConfig` *is* present but `environment` is an unrecognised string. |
| `funktor/inspect/src/jvmMain/kotlin/introspection/api/IntrospectionApi.kt:74-75` | `isProduction`, `isDevelopment` | Both values are echoed verbatim as `ConfigInfoEntry` rows in a superuser-only introspection response (route guarded by `.authorize { isSuperUser() }`, `IntrospectionApi.kt:65-66`) | **N/A as a security gate** — this is read-only display to an already-privileged caller, not a permission/data gate. It will start reporting correctly (rather than misleadingly) once this task lands; no code change needed here beyond the shared flags being fixed. |

`isLocalDev` and `isQa` have **no other call sites** anywhere in the repo besides the definitions
above and the two test files. `isDevelopment` has no gating call site today — only the introspection
display above. (The `fixtures-env-gate` task proposes switching `fixtures_module.kt` to gate on
`isDevelopment` instead of `!isProduction`, but that switch hasn't landed yet — confirmed by reading
`fixtures_module.kt` above, it still reads `isProduction`.)

Two more `environment`-adjacent sites were checked and are **out of scope** — they read the raw
string, not the boolean flags, so they aren't part of this fail-open bug:
`funktor/cluster/src/jvmMain/kotlin/locks/GlobalServerId.kt:14` (folds the raw string into a server
ID) and `funktor/messaging/src/jvmMain/kotlin/overrides.kt:31` (puts the raw string in an email
subject/banner prefix for dev-mode mail).

### 3. Where `environment` comes from at runtime

- Boot path: `App.run()` (`funktor/core/src/jvmMain/kotlin/app.kt:90-119`) builds Ktor's own
  `CommandLineConfig(args)` (`app.kt:98`), converts it via `AppConfig.of(type, ktorConfig.environment.config.toMap())`
  (`app.kt:100`), then reads `config.ktor.deployment.environment` purely as a resolved string
  (`app.kt:102`, used only for log lines at `app.kt:111,117`). There is no validation or enum
  parsing anywhere on this path — whatever string ends up in the resolved HOCON tree at that key is
  used verbatim.
- `AppConfig.loadEnv(env)` (`funktor/core/src/jvmMain/kotlin/config/AppConfig.kt:46-51`) is a
  **different** notion of "env" — it's a filename selector (`config/application.{env}.conf`, built
  at `AppConfig.kt:114`), not the `ktor.deployment.environment` value inside the loaded file.
  `TestBed.createTestBed(env = "test")` (`funktor/testing/src/jvmMain/kotlin/setup.kt:20-22`) uses
  this to pick `application.test.conf`, and that file *also* happens to set
  `deployment.environment = "test"` inside itself
  (`funktor-demo/server/src/main/resources/config/application.test.conf:12` and
  `funktor/all/src/jvmTest/resources/config/application.test.conf:9`) — but nothing enforces the
  file-selector string and the in-file `environment` value agree. **OPEN QUESTION**: should
  `loadEnv`/config loading assert they match, so a copy-pasted config file can't silently carry the
  wrong `environment` value into a differently-named file? Not required by this task's scope, but
  worth raising with the domain-expert reviewer.
- No `${ENV}`-style HOCON substitution is used for the `environment` key in any committed `.conf`
  file — `grep -rn "environment" **/*.conf` across the whole repo returns exactly three hits, all
  literal strings: `application.dev.conf:8` (`environment = "dev"`), and
  `application.test.conf:12` / `funktor/all/.../application.test.conf:9` (both `environment = "test"`).
- **No committed config file uses `"prod"`, `"live"`, `"production"`, `"staging"`, or `"qa"` for
  `deployment.environment` anywhere in this repo.** Those names appear *only* inside
  `KtorConfigSpec.kt` and `ApiStatusPagesSpec.kt` as hypothetical test fixtures, and `"prod"`
  appears once more as the hardcoded default (`KtorConfig.kt:12`). There is no `Dockerfile`, no
  Kubernetes/deploy manifest, and no CI workflow in this repo that sets an environment variable
  feeding this value (checked: `find` for `Dockerfile*`, any `k8s`/`kubernetes`/`deploy` path, and
  `.github/` all came back empty or unrelated — the one `deploy-peekandpoke-io.sh` script is for the
  separate docs-site and doesn't touch funktor config). **OPEN QUESTION, unconfirmed from source:**
  the actual string(s) used for real prod/staging deployments live outside this repository (or
  don't exist yet for this still-in-development SaaS product). The recommended design in item 4
  makes this largely moot — since unrecognised strings default to production either way, it's safe
  to proceed without confirming the exact ops-side spelling, but it would still be good to check
  with whoever owns deployment before this ships.
- Real environment names actually observed in-repo: **`dev`, `test`** (plus the code default
  `prod`). `live`, `qa`, `staging`, `prd`, `production-eu` etc. are not used by any committed
  config — they exist only as hypotheticals in the two test specs.

### 4. Recommended fix design

**Option A (recommended, minimal diff) — anchor both flags to the one allow-list that already
fails closed, `isDevelopment`, instead of maintaining a second, independently-fallible allow-list
for production:**

```kotlin
val isLocalDev: Boolean get() = deployment.environment.lowercase() == "dev"
val isTest: Boolean get() = deployment.environment.lowercase() == "test"
val isQa: Boolean get() = deployment.environment.lowercase().startsWith("qa")
val isDevelopment: Boolean get() = isLocalDev || isTest || isQa   // unchanged — already fail-closed

val isProduction: Boolean get() = !isDevelopment      // was: environment in listOf("live", "prod", "production")
val isNotProduction: Boolean get() = isDevelopment    // was: !isProduction
```

For any unrecognised string (`staging`, `prd`, `production-eu`, `""`, `"${ENV}"`, trailing
whitespace, mixed case not covered above): `isDevelopment == false` ⇒ `isProduction == true` and
`isNotProduction == false`. This satisfies every Spec checkbox above with a two-line diff, and
removes the separate "list of production names" entirely — there is structurally only one allow-list
left in the type, and getting *that* one wrong now fails toward the restrictive side, not the
permissive one. `isProduction` and `isNotProduction` remain complements of each other, but that's
now safe because they're both complements of the trusted allow-list rather than of each other's
independently-maintained list.

**Option B (stretch goal, larger behavior change) — parse into a sealed type at config load and
fail startup on an unrecognised value**, per the "Preferably" checklist item:

```kotlin
sealed interface DeploymentEnv {
    val raw: String

    data class Dev(override val raw: String) : DeploymentEnv
    data class Test(override val raw: String) : DeploymentEnv
    data class Qa(override val raw: String) : DeploymentEnv
    data class Production(override val raw: String) : DeploymentEnv  // covers prod/live/production AND staging, see item 5

    companion object {
        fun parseOrFail(raw: String): DeploymentEnv {
            val lower = raw.trim().lowercase()
            return when {
                lower == "dev" -> Dev(raw)
                lower == "test" -> Test(raw)
                lower.startsWith("qa") -> Qa(raw)
                lower.isNotEmpty() -> Production(raw)  // default-safe, not "unknown" — never throws by default
                else -> error("Empty deployment environment — refusing to start")
            }
        }
    }
}
```

This is a **bigger** change than Option A: turning a bad/blank string into a boot failure is a
behavior change that needs explicit ops sign-off (it can turn a previously-"working" misconfigured
deploy into one that won't start at all). Recommend shipping Option A first — it closes the
disclosure and fixture-installation holes immediately with minimal risk — and tracking Option B as
a follow-up decision for the domain-expert review pass rather than deciding it unilaterally here.

### 5. Should `staging` be treated as production?

**Recommendation: yes**, for disclosure purposes — and under Option A this falls out for free with
no special-case entry needed: `staging` is not `"dev"`, not `"test"`, and does not start with
`"qa"`, so `isDevelopment == false` ⇒ `isProduction == true` automatically. Rationale: staging
environments typically hold production-shaped or copied data, are reachable by less-trusted testers
or partners, and the cost of over-classifying staging as production (slightly less verbose error
messages there) is far lower than the cost of under-classifying it (stack traces / auth-rule
internals leaking on a host that looks and often behaves like production). This matches the
recommendation already recorded in this file's original Spec section.

### 6. Tests that assert current (soon-to-be-wrong) behaviour and must be inverted

**`funktor/core/src/jvmTest/kotlin/config/ktor/KtorConfigSpec.kt`:**

- `"non-production environments must not be treated as production"` (`KtorConfigSpec.kt:30-36`) —
  currently asserts `isProduction shouldBe false` for `listOf("dev", "test", "qa", "qa-2", "staging", "")`.
  Split this: keep `dev`/`test`/`qa`/`qa-2` (still correctly non-production under Option A) in this
  test; **remove** `staging` and `""` from it and move them into a new test, e.g.
  `"unrecognised environments must be treated as production"`, asserting
  `isProduction shouldBe true` **and** `isNotProduction shouldBe false` for at least
  `staging, prd, preprod, production-eu, "", "${ENV}", " prod "` (trailing/leading whitespace).
- `"environments must not be classified as production by prefix or substring"`
  (`KtorConfigSpec.kt:38-44`) — currently asserts `isProduction shouldBe false` for
  `production-mirror, preprod, prod-clone, not-live`. **Fully inverts** under Option A: none of
  these are on the `isDevelopment` allow-list, so they all become `isProduction == true` — not
  because of substring matching (Option A has no substring logic for production at all), but
  because they're simply unrecognised. Rename the test (its current name/rationale — "must not
  match by substring" — no longer describes the invariant) to something like
  `"environments that merely look like production default to production anyway"` and flip every
  assertion to `shouldBe true`.
- `"production environments must be recognised, in any casing"` (`:15-22`),
  `"the default environment must be treated as production"` (`:24-28`),
  `"development environments must be recognised"` (`:46-52`), and
  `"production must never also count as development"` (`:54-65`) are **unaffected** — Option A
  preserves all of these invariants unchanged.

**`funktor/rest/src/jvmTest/kotlin/ApiStatusPagesSpec.kt`:**

- `"environments that merely look like production must not be treated as production"`
  (`ApiStatusPagesSpec.kt:48-54`) — currently asserts `exposesStackTraces shouldBe true` for
  `staging, preprod, prod-clone, production-mirror`. **Invert to `shouldBe false`.** Consider
  folding these four strings directly into the existing
  `"production environments must never expose stack traces"` list (`:23-29`) instead of keeping a
  separate test, since after the fix they behave identically to the real production names — a
  separate test now only makes sense if it exists to document *why* (unrecognised-defaults-to-prod),
  not to assert different behavior.
- `"an unknown environment must be treated as production"` (`:17-21`, null-config case),
  `"production environments must never expose stack traces"` (`:23-29`),
  `"the default configuration must not expose stack traces"` (`:31-34`),
  `"the empty configuration must not expose stack traces"` (`:36-38`), and
  `"non-production environments may expose stack traces"` (`:40-46`, `dev/test/qa/qa-eu`) are
  **unaffected**.

**No existing test coverage at all** for the two riskiest call sites in the blast-radius table —
confirmed by search: there is no `*fixtures*Spec*`/`*Fixtures*Test*` file and no
`*senders*Spec*`/`*EmailSender*Spec*` file anywhere in the repo. `fixtures_module.kt:16` is owned by
`.claude/tasks/20260720-fixtures-env-gate.md` (should add kontainer-resolution tests there, per that
task's own Test evidence section). `senders.kt:13` and `server.kt:25` have no task tracking new
tests yet — recommend adding at least one regression test for `senders.kt`'s
`applyDevConfig`/`isTest` interaction as part of this task's Test evidence, since it will silently
change behavior (real sender vs. `NullEmailSender`) the moment `KtorConfig`'s flags change, with
nothing currently watching it.

## Implementation notes

Root cause shared with:
- `.claude/tasks/20260720-fixtures-env-gate.md` (finding 4) — same inversion, different module;
  that task's gate should move from `isProduction` to `isDevelopment`, which only becomes trustworthy
  once this task lands (see item 2's table row for `fixtures_module.kt:16`)
- finding 5 in the audit doc (`funktor/rest/src/jvmMain/kotlin/respond.kt:57`), which is fixed for
  free by this change (no code change needed at that call site itself — it already reads
  `isNotProduction`, it just inherits a fixed flag)

**BREAKING CHANGE FOR EXISTING TESTS.** See item 6 above for the exhaustive, line-numbered list of
assertions in `KtorConfigSpec.kt` and `ApiStatusPagesSpec.kt` that encode today's permissive
semantics and must be inverted, split, or renamed as part of this task — they were written before
this finding existed and are not a desired invariant.

Also consider `respond.kt:57`: `appConfig` there throws when no kontainer is on the call, turning a
clean 401 into a 500 (confirmed: `core_module.kt:55` — `ApplicationCall.appConfig` is a non-nullable
getter over `kontainer.get(...)`, which throws if no kontainer is present, unlike
`ApiStatusPages.kt`'s `config?.ktor?.isNotProduction == true` pattern). Switching `respond.kt:57` to
`kontainerOrNull?.getOrNull(AppConfig::class)?.ktor?.isNotProduction == true` mirrors the
`ApiStatusPages` fix and removes that edge case. This is a small, separate, low-risk change bundled
into the same task since it touches the same call site and same flag.

Open questions surfaced during analysis, for the domain-expert/security review pass rather than
unilateral decisions here:
1. Should `loadEnv`'s file-selector string and the loaded file's internal `deployment.environment`
   value be asserted to match at load time? (item 3)
2. Ship Option B (fail-startup sealed type) now, or track it as a separate follow-up task after
   Option A lands and proves out? (item 4)
3. The stale `"local"` allow-list reference in an earlier draft of this task's Spec — confirm it's
   not an actual requirement before closing this out (item 1 / Spec first bullet).

## Test evidence

- [ ] Unit tests for every classification helper against: the three production names, casing
      variants, the dev/test/qa names, and a table of unknown strings (`staging`, `prd`,
      `production-eu`, `""`, `"${ENV}"`, whitespace) — see item 6 for exact test names to add/split
- [ ] Inverted/split/renamed assertions in the two specs listed in item 6
- [ ] New regression test for `funktor/messaging/src/jvmMain/kotlin/senders/senders.kt:13`
      (`applyDevConfig`) covering an unrecognised environment with `devOverrides.mailing` present —
      currently zero coverage on this call site
- [ ] End-to-end: not required (pure config logic, no storage)
- [ ] Full test command(s) run + green: `./gradlew :funktor:core:jvmTest :funktor:rest:jvmTest :funktor:messaging:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
