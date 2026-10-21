# Upgrade to Kotlin 2.4.21, refresh the dependency set, release 0.111.0

**Status:** DONE (archived 2026-10-18) — `fb1b93f8`, `0dc2df67`, release `54362e3e` / `v0.111.0`
**Plan:** none — maintainer request, 2026-10-18, right after the 0.110.0 release
**Security-critical:** no
**Previous round:** `docs/tasks-archive/2026-07/20260729-kotlin-24-upgrade.md` — same playbook

## Spec

- [x] Kotlin 2.4.10 → **2.4.21** (stable, released 2026-10-08) in `gradle.properties` and `docs-site/src/data/site.ts`
- [x] Every other dependency to its newest version that is compatible with Kotlin 2.4.21 — holds named below with a reason
- [x] kotlinx-datetime attempted LAST — **held at 0.6.2**, see Step 2, in its own commit, so it can be reverted alone (maintainer: it caused trouble in
      the past; 0.7.0 changed formatting and moved `Instant`/`Clock` to `kotlin.time`)
- [x] Full suite green and matching the baseline below, per target
- [x] Release 0.111.0 prepared — `54362e3e`, tag `v0.111.0`; docs build shows `Version: 0.111.0. Kotlin: 2.4.21.` (version bump + tag; the maintainer builds, pushes and publishes)
- [x] The three Kraft example repos on Kotlin 2.4.21 / Ultra 0.111.0 / Gradle 9.7.1 (see "Example repos")

## Baseline — the maintainer's green full run on `9fe1cf72` (0.110.0), 2026-10-18

Harvested from `build/test-results/**/TEST-*.xml` before anything changed: **14675 tests, 7 skipped, 0 failures, 0 errors
over 68 targets.** Diff against it after the upgrade — a target whose count
drops (especially to 0) is a regression even when the build is green.

<details><summary>Per-target baseline (68 targets)</summary>

| module | target | tests | skipped |
|---|---|---|---|
| `funktor-demo/server` | test | 47 | 0 |
| `funktor/all` | jvmTest | 158 | 0 |
| `funktor/auth` | jsBrowserTest | 4 | 0 |
| `funktor/auth` | jvmTest | 141 | 0 |
| `funktor/cluster` | jvmTest | 56 | 0 |
| `funktor/codegen` | test | 88 | 0 |
| `funktor/core` | jvmTest | 774 | 2 |
| `funktor/insights` | jvmTest | 76 | 0 |
| `funktor/logging` | jvmTest | 12 | 0 |
| `funktor/messaging` | jvmTest | 67 | 0 |
| `funktor/rest` | jvmTest | 116 | 0 |
| `funktor/saas` | jsBrowserTest | 9 | 0 |
| `funktor/saas` | jvmTest | 50 | 0 |
| `karango/core` | test | 1649 | 0 |
| `karango/ksp` | test | 11 | 0 |
| `kraft/addons/avatars` | jsBrowserTest | 4 | 0 |
| `kraft/addons/browserdetect` | jsBrowserTest | 3 | 0 |
| `kraft/addons/chartjs` | jsBrowserTest | 20 | 0 |
| `kraft/addons/datetime` | jsBrowserTest | 2 | 0 |
| `kraft/addons/jwtdecode` | jsBrowserTest | 3 | 0 |
| `kraft/addons/marked` | jsBrowserTest | 3 | 0 |
| `kraft/addons/pixijs` | jsBrowserTest | 4 | 0 |
| `kraft/addons/prismjs` | jsBrowserTest | 3 | 0 |
| `kraft/addons/signaturepad` | jsBrowserTest | 3 | 0 |
| `kraft/addons/threejs` | jsBrowserTest | 4 | 1 |
| `kraft/core` | jsBrowserTest | 216 | 0 |
| `kraft/core` | jvmTest | 210 | 0 |
| `kraft/core-tests` | jsBrowserTest | 412 | 0 |
| `kraft/semanticui` | jsBrowserTest | 5 | 0 |
| `monko/core` | test | 249 | 0 |
| `monko/ksp` | test | 6 | 0 |
| `mutator/core` | jvmTest | 154 | 0 |
| `tooling` | test | 22 | 0 |
| `tooling/i18n-fixture` | jsBrowserTest | 7 | 0 |
| `tooling/i18n-fixture` | jvmTest | 6 | 0 |
| `ultra/cache` | jsBrowserTest | 124 | 1 |
| `ultra/cache` | jvmTest | 138 | 1 |
| `ultra/cache` | linuxX64Test | 123 | 1 |
| `ultra/codegen` | test | 325 | 0 |
| `ultra/common` | jsBrowserTest | 192 | 1 |
| `ultra/common` | jvmTest | 378 | 0 |
| `ultra/common` | linuxX64Test | 186 | 0 |
| `ultra/datetime` | jsBrowserTest | 1753 | 0 |
| `ultra/datetime` | jvmTest | 1792 | 0 |
| `ultra/datetime` | linuxX64Test | 1752 | 0 |
| `ultra/fixture` | jsBrowserTest | 24 | 0 |
| `ultra/fixture` | jvmTest | 23 | 0 |
| `ultra/html` | jvmTest | 28 | 0 |
| `ultra/i18n` | jsBrowserTest | 30 | 0 |
| `ultra/i18n` | jvmTest | 29 | 0 |
| `ultra/kontainer` | test | 186 | 0 |
| `ultra/log` | jvmTest | 66 | 0 |
| `ultra/maths` | jsBrowserTest | 115 | 0 |
| `ultra/maths` | jvmTest | 114 | 0 |
| `ultra/maths` | linuxX64Test | 114 | 0 |
| `ultra/model` | jsBrowserTest | 77 | 0 |
| `ultra/model` | jvmTest | 93 | 0 |
| `ultra/reflection` | test | 56 | 0 |
| `ultra/remote` | jsBrowserTest | 168 | 0 |
| `ultra/remote` | jvmTest | 167 | 0 |
| `ultra/security` | jvmTest | 208 | 0 |
| `ultra/semanticui` | jsBrowserTest | 30 | 0 |
| `ultra/semanticui` | jvmTest | 29 | 0 |
| `ultra/slumber` | jvmTest | 1241 | 0 |
| `ultra/streams` | jsBrowserTest | 100 | 0 |
| `ultra/streams` | jvmTest | 69 | 0 |
| `ultra/streams` | linuxX64Test | 66 | 0 |
| `ultra/vault` | jvmTest | 285 | 0 |

</details>

## Versions — looked up 2026-10-18

Sources: `repo1.maven.org` `maven-metadata.xml` (newest stable), the npm registry `dist-tags.latest`,
`services.gradle.org`, and the KGP/Gradle compatibility table at kotlinlang.org/docs/gradle-configure-project.html.

### Toolchain

| What | Now | Target | Note |
|---|---|---|---|
| Kotlin / KGP | 2.4.10 | 2.4.21 | 2.5.0 is Beta only |
| KSP | 2.3.10 | 2.3.12 | KSP is plain semver since 2.3.7 — not tied to Kotlin's number |
| Gradle | 9.5.0 | 9.7.1 | KGP 2.4.20–2.4.21 fully supports Gradle 7.6.3–9.7.0; 9.8.1 is out of range |
| Dokka | 2.2.0 | 2.2.0 | 2.3.0 is Beta only |
| vanniktech maven-publish | 0.33.0 | 0.37.0 | the maintainer publishes with it — verify `publishToMavenLocal` |
| kotest (plugin + libs) | 6.2.3 | 6.2.6 | |
| kctfork (ksp compile-testing) | 0.13.0 | 0.14.0 | |

### Kotlin libraries

| What | Now | Target | Note |
|---|---|---|---|
| kotlinx-coroutines | 1.11.0 | 1.11.0 | already newest |
| kotlinx-serialization | 1.11.0 | 1.11.0 | 1.12.0 is RC only |
| kotlinx-html | 0.12.0 | 0.12.0 | already newest |
| kotlin-wrappers (css, js) | 2026.7.7 | 2026.10.1 | |
| Ktor | 3.5.1 | 3.6.0 | |
| mongodb driver | 5.9.1 | 5.13.0 | |
| kotlinx-datetime | 0.6.2 | **held 0.6.2** | 0.8.0 breaks every named zone on JS — see Step 2 |

### JVM libraries

| What | Now | Target | Note |
|---|---|---|---|
| arangodb-java-driver | 7.26.0 | 7.28.0 | |
| aws sdk | 2.49.5 | 2.56.0 | |
| snakeyaml | 2.6 | 2.7 | also the copy in `buildSrc/build.gradle.kts` |
| google-api-client | 2.9.0 | 2.9.1 | |
| logback | 1.6.1 / Test 1.5.32 | 1.6.5 | `Deps.Test` had drifted to 1.5.32 |
| slf4j | 2.0.17 | 2.0.20 | 2.1.0 is alpha |
| java-diff-utils | 4.16 | 4.18 | |
| sendgrid | 5.0.0-rc.1 | hold | ahead of stable 4.10.3 on purpose |
| apns | 1.0.0.Beta6 | hold | the "newer" 0.2.3 is a lower version |

### npm (Kraft addons) — maintainer: minor/patch only, majors held

| What | Now | Target | Held major |
|---|---|---|---|
| preact | 10.26.4 | 10.29.8 | 11.0.1 — Kraft's renderer, its own task |
| @js-joda/timezone | 2.23.0 | 2.26.0 | 3.1.0 |
| dompurify | 3.3.2 | 3.4.17 | |
| pixi.js | 8.17.1 | 8.22.0 | |
| three | 0.183.2 | 0.186.1 | (0.x: every minor is a three.js release) |
| signature_pad | 5.1.3 | 5.1.4 | |
| marked | 15.0.12 | 15.0.12 | 18.1.1 |
| jwt-decode | 3.1.2 | 3.1.2 | 4.0.0 |
| pdfjs-dist | 4.10.38 | 4.10.38 | 6.4.299 |

### Not touched, on purpose

- **WebJars, all of them** (maintainer: careful, funktor references them). The used ones are served by hard-coded
  in-jar paths — `funktor/staticweb/src/main/kotlin/resources/common/common_resources.kt:25-42`,
  `.../resources/prismjs/index.kt:27-36`. A bump that moves or drops a file still compiles and only 404s at runtime, and
  no test loads those paths. Available: jquery 4.0.0 (major; Fomantic 2.x wants jQuery 3), fomantic-ui-css 2.9.0,
  prismjs 1.30.0, glidejs 3.6.0. Any of these needs a browser check of a staticweb page.
- **Deps entries no build file references** — nothing compiles against them, so a bump is unverifiable: kotlinpoet,
  firebase-admin, excelkt, qrcode-kotlin, okhttp, commonmark, tika, flying-saucer, pdfbox, apns, avatar-generator,
  kotlin-csv, `Npm.konva`, `Npm.pdfJs`. Candidates for deletion — maintainer's call, not decided.

## Implementation notes

### Step 1 — toolchain + libraries (everything except kotlinx-datetime)

Compile sweep (`compileKotlinJvm … compileTestKotlinLinuxX64 --continue`, 476 tasks): **one error, five warnings**, all
caused by the upgrade, all fixed:

- **Kotlin 2.4.21 rejects `:` in a backticked JVM name.** `FxHostileOrder` (`funktor/codegen/src/test/kotlin/rest_fixtures.kt:261`)
  had the constant `` `a' | string, x: any, z: 'b` ``; now `` `a' | string | 'b` ``. Still hostile in the way the spec
  needs: emitted raw, it widens the union to `'a' | string | 'b'`. **Mutation-checked:** with the quote escape removed
  from `tsStringLiteral` (`ultra/codegen/src/main/kotlin/ts/TsLiterals.kt:18`), exactly "enum constant names are
  ESCAPED…" goes RED (88 tests, 1 failed); restored and `cmp`-verified.
- **2.4.20+ flags a value at the end of a `Unit` lambda.** `GetAndSet.of(setter: (P) -> Unit)` — five test call sites
  ended in `; it` (`ultra/common/src/commonTest/kotlin/CommonFixesSpec.kt:93-94`, `GetAndSetSpec.kt:69,82,95`). Dropped.
- **arangodb 7.28 deprecates `CollectionEntity.status`** ("Removed in ArangoDB 4.0"). Still reported by the 3.x servers
  in use and only feeds the informational `RepositoryStats.status`; suppressed at the one call site with that reason
  (`karango/core/src/main/kotlin/vault/EntityRepository.kt`). Revisit when ArangoDB 4 lands.
- **A shadowed test extension** — `TypeWalkerSpec`'s private `TypeModel.declFor(KType)` looked up by `TypeId.of(type)`,
  but the member `TypeModel.declFor(KType)` (`ultra/codegen/src/main/kotlin/model/TypeModel.kt:79`, lookup by class)
  always won. Deleted — behaviour-neutral, the spec has always run against the member. If the author meant the
  `TypeId` lookup (generic instantiations), that intent was never exercised.

`./gradlew wrapper --gradle-version 9.7.1` regenerated the wrapper jar and scripts; `kotlinUpgradeYarnLock` refreshed
`kotlin-js-store/yarn.lock`.

### Step 2 — kotlinx-datetime 0.8.0: attempted, HELD at 0.6.2 (maintainer, 2026-10-18)

Green on JVM (1792) and native (1752); **in the browser every named zone fails** — `:ultra:datetime:jsBrowserTest` ran
595 of 1753 and failed 518 (`Invalid zone ID`). 0.7+ on JS reads the packed IANA data through js-joda's
`getTzdbData()` hook and ignores the Intl-backed `ZoneRulesProvider` that `ultra/datetime` installs instead of shipping
the database. Slumber also went RED where expected: `LocalDateTimeCodecSpec.kt:70,92` pin the `"Z"` that
`TimeZone.UTC.id` becomes `"UTC"` — a persisted wire change.

Reverted (`git apply -R`), and the work kept: root cause, options, the full migration diff and the persistence items
are in **`docs/tasks-archive/2026-10/20261018-kotlinx-datetime-js-timezones.md`** (+ `.patch`, verified to apply on `fb1b93f8`).
Kept from step 2 only `@Suppress("DANGEROUS_CHARACTERS")` on `FxHostileOrder` — a Kotlin 2.4.21 warning on the `|` in the
fixture name, unrelated to datetime; `:funktor:codegen:test` 88/88 after it, warning-free.

## Example repos

The 0.110.0 bump of the three example repos (Kotlin 2.4.10, Gradle 9.5.0, two API-break fixes in remote and router) is
uncommitted in those repos and is folded into this upgrade instead: they move straight to 0.111.0.

State 2026-10-18: all three on Kotlin 2.4.21, KSP 2.3.12, Ultra 0.111.0, Gradle 9.7.1 (wrapper regenerated); remote
also on Ktor 3.6.0, in step with Ultra. Trial `jsBrowserProductionWebpack` against 0.109.0 (built with Kotlin 2.4.x):
green in all three. Once 0.111.0 was served by Maven Central: `kotlinUpgradeYarnLock` + clean
`jsBrowserProductionWebpack` against 0.111.0, green in all three; committed and pushed at the maintainer's request —
helloworld `60bac94`, remote `d5c4a89`, router `620a717`.

**No docs task:** a dependency upgrade, no Ultra API changed. The docs' `kotlinVersion` moved with it. Found on the
way and fixed in the same session: the Kraft forms docs called `formCtrl.validate()` (now `suspend`) and the removed
`ifValidate { }` from `onClick` — five snippets and the API table, in `forms.astro` and the `kraft.md` mirror.

## Test evidence

- [x] Compile sweep (all JVM/JS/native main + test targets, `--continue`): 0 errors, 0 warnings after step 1's fixes
- [x] `./gradlew clean build --continue` with both DBs up, after step 1: BUILD SUCCESSFUL, **14675 tests over 68
      targets, 0 failures — identical per target to the baseline**
- [x] Mutation check on the changed security fixture (`FxHostileOrder`): RED for the right spec, restored, `cmp`-verified
- [x] maven-publish 0.37.0: `:ultra:common:publishToMavenLocal` under a throwaway version produced jar, sources, javadoc,
      module and POM, all signed, POM on kotlin-stdlib 2.4.21; trial version removed from the local repo
      (`~/.m2/swisslife-repository`, per `settings.xml`) and its metadata restored. The maintainer publishes.

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|

**Verdict:** gate NOT RUN — a dependency upgrade, not a feature; the previous one
(`docs/tasks-archive/2026-07/20260729-kotlin-24-upgrade.md`) ran none either. The only production-code change is one
`@Suppress("DEPRECATION")` in `karango/core/src/main/kotlin/vault/EntityRepository.kt`; everything else is build
configuration and test fixtures, covered by the full suite matching the baseline per target. Maintainer may still ask
for it.
