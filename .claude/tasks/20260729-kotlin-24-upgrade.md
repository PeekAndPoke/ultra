# Upgrade to Kotlin 2.4.10, refresh the dependency set

**Status:** TODO — intended for a fresh session, this is a whole-repo change
**Plan:** none — decided during the `ultra/common` scan, 2026-07-29
**Security-critical:** no

## Why

Kotlin is on **2.3.10** here (`buildSrc/src/main/kotlin/Deps.kt:20`). Latest stable is **2.4.10**,
released 2026-07-14 — checked against https://kotlinlang.org/docs/releases.html, not from memory.
2.4.0 was the language release (2026-06-03), 2.4.10 its bug-fix follow-up. 2.4.20 is planned for
September 2026, so 2.4.10 is the right target today.

Every version number below is what is in the repo **now**. Look each one up before changing it —
do not trust these as current, and do not trust a model's recollection of "latest". The project rule
is explicit: look up the newest version online before bumping anything.

## STEP 0 — establish a green baseline BEFORE touching a version

Non-negotiable, and the reason this needs its own session. Without a known-good starting point you
cannot tell an upgrade regression from something that was already broken.

```
docker start mongodb arangodb      # storage-touching suites need both
./gradlew allTests --continue
```

Then record, per module, the counts read from `build/test-results/**/TEST-*.xml` — **not** from the
gradle summary. Two traps that apply here (see `CLAUDE.md` → Verification traps):

- kotest ignores `--tests`, so a filtered invocation proves nothing.
- The XML mis-attributes *which* test failed; the counts are reliable, the per-test names are not.
  Use the gradle console output to identify a specific failure.

Watch for suites that report **0 tests** — `mutator/core`'s `jsBrowserTest` does, because all its
specs live in `jvmTest`. A zero-count target is not a passing target. See
`.claude/tasks/20260729-mutator-test-coverage-platforms.md`.

### Baseline recorded 2026-07-29 — ALL GREEN

Run by the maintainer with both DBs up, on commit `a4ba82a7`. Gradle's console reported
**13655 passed, 6 ignored, 0 failed**. Harvested from the XML: **13662 tests, 7 ignored, 0
failures across 65 targets** — the ~7 test delta is a console-vs-XML counting difference,
not a discrepancy in outcome.

This is the table to diff against after the upgrade. A target whose count drops — especially to
0 — is a regression even if the build is green.

| module | target | tests | ignored |
|---|---|---|---|
| `funktor-demo/server` | test | 47 | 0 |
| `funktor/all` | jvmTest | 138 | 0 |
| `funktor/auth` | jsBrowserTest | 8 | 0 |
| `funktor/auth` | jvmTest | 141 | 0 |
| `funktor/cluster` | jvmTest | 56 | 0 |
| `funktor/core` | jvmTest | 772 | 2 |
| `funktor/logging` | jvmTest | 12 | 0 |
| `funktor/messaging` | jvmTest | 65 | 0 |
| `funktor/rest` | jvmTest | 109 | 0 |
| `funktor/saas` | jsBrowserTest | 9 | 0 |
| `funktor/saas` | jvmTest | 50 | 0 |
| `karango/core` | test | 1649 | 0 |
| `karango/ksp` | test | 9 | 0 |
| `kraft/addons/avatars` | jsBrowserTest | 4 | 0 |
| `kraft/addons/browserdetect` | jsBrowserTest | 3 | 0 |
| `kraft/addons/chartjs` | jsBrowserTest | 3 | 0 |
| `kraft/addons/datetime` | jsBrowserTest | 2 | 0 |
| `kraft/addons/jwtdecode` | jsBrowserTest | 3 | 0 |
| `kraft/addons/marked` | jsBrowserTest | 3 | 0 |
| `kraft/addons/pixijs` | jsBrowserTest | 4 | 0 |
| `kraft/addons/prismjs` | jsBrowserTest | 3 | 0 |
| `kraft/addons/signaturepad` | jsBrowserTest | 3 | 0 |
| `kraft/addons/threejs` | jsBrowserTest | 4 | 1 |
| `kraft/core-tests` | jsBrowserTest | 394 | 0 |
| `kraft/core` | jsBrowserTest | 216 | 0 |
| `kraft/core` | jvmTest | 210 | 0 |
| `kraft/semanticui` | jsBrowserTest | 5 | 0 |
| `monko/core` | test | 249 | 0 |
| `monko/ksp` | test | 4 | 0 |
| `mutator/core` | jvmTest | 154 | 0 |
| `tooling` | test | 22 | 0 |
| `tooling/i18n-fixture` | jsBrowserTest | 7 | 0 |
| `tooling/i18n-fixture` | jvmTest | 6 | 0 |
| `ultra/cache` | jsBrowserTest | 102 | 1 |
| `ultra/cache` | jvmTest | 111 | 1 |
| `ultra/cache` | linuxX64Test | 101 | 1 |
| `ultra/common` | jsBrowserTest | 167 | 1 |
| `ultra/common` | jvmTest | 332 | 0 |
| `ultra/common` | linuxX64Test | 161 | 0 |
| `ultra/datetime` | jsBrowserTest | 1732 | 0 |
| `ultra/datetime` | jvmTest | 1771 | 0 |
| `ultra/datetime` | linuxX64Test | 1731 | 0 |
| `ultra/fixture` | jsBrowserTest | 24 | 0 |
| `ultra/fixture` | jvmTest | 23 | 0 |
| `ultra/html` | jvmTest | 28 | 0 |
| `ultra/i18n` | jsBrowserTest | 26 | 0 |
| `ultra/i18n` | jvmTest | 25 | 0 |
| `ultra/kontainer` | test | 186 | 0 |
| `ultra/log` | jvmTest | 43 | 0 |
| `ultra/maths` | jsBrowserTest | 115 | 0 |
| `ultra/maths` | jvmTest | 114 | 0 |
| `ultra/maths` | linuxX64Test | 114 | 0 |
| `ultra/model` | jsBrowserTest | 77 | 0 |
| `ultra/model` | jvmTest | 93 | 0 |
| `ultra/reflection` | test | 61 | 0 |
| `ultra/remote` | jsBrowserTest | 168 | 0 |
| `ultra/remote` | jvmTest | 167 | 0 |
| `ultra/security` | jvmTest | 142 | 0 |
| `ultra/semanticui` | jsBrowserTest | 30 | 0 |
| `ultra/semanticui` | jvmTest | 29 | 0 |
| `ultra/slumber` | jvmTest | 1219 | 0 |
| `ultra/streams` | jsBrowserTest | 62 | 0 |
| `ultra/streams` | jvmTest | 31 | 0 |
| `ultra/streams` | linuxX64Test | 28 | 0 |
| `ultra/vault` | jvmTest | 285 | 0 |

**Known-benign zero:** `mutator/core` has no `jsBrowserTest` entry at all, because every one of its
specs lives in `jvmTest`. That is pre-existing and tracked separately; do not read its absence as an
upgrade regression.

## STEP 1 — Kotlin and the toolchain

| Dep | Current | Notes |
|---|---|---|
| `kotlinVersion` | 2.3.10 | → 2.4.10 |
| KSP (`Deps.Ksp.version`) | 2.3.6 | **must match the Kotlin version** — KSP is versioned `<kotlin>-<ksp>`. Find the release built for 2.4.10. |
| `dokkaVersion` | 2.1.0 | check for a release matching 2.4.x |
| `mavenPublishVersion` | 0.33.0 | |
| `jvmTarget` | JVM_17 | `Deps.kt:37`. 2.4.0 adds full Java 26 support; decide whether to move the toolchain or stay on 17. Moving it is a separate decision with its own blast radius — do not fold it in silently. |

KSP is the one most likely to block: it lags Kotlin releases and the KSP2 migration has caught this
repo before (`CLAUDE.md` warns about kapt flakiness with `--rerun-tasks`).

## STEP 2 — the kotlinx / JetBrains set

These track Kotlin and usually need moving together.

| Dep | Current |
|---|---|
| `kotlinx-coroutines` | 1.10.2 |
| `kotlinx-serialization` | 1.10.0 |
| `kotlinx-datetime` | 0.6.2 |
| `kotlinx-html` | 0.12.0 |
| `kotlin-wrappers` | 2026.3.7 |
| `kotlin-js` wrappers | 2026.3.8 |
| Ktor | 3.4.2 |

### Do NOT bump `kotlinx-datetime` unless forced

Decided 2026-07-29: leave it at 0.6.2. It is pre-1.0, has broken between minors before, and
`ultra/datetime` wraps it heavily — that module alone carries 5234 tests, and ten other modules
depend on it.

The only question that can override this: **does 0.6.2 actually work against Kotlin 2.4.10?** Find
that out first. If it does, leave it alone and let the rest of the upgrade land. If it does not, the
bump becomes mandatory rather than optional, and then:

- read the changelog for every version in between, not just the target
- run `:ultra:datetime:allTests` on its own before anything else
- then the ten dependents: `ultra/maths`, `ultra/model`, `ultra/slumber`, `ultra/cache`,
  `ultra/vault`, `karango/core`, `monko/core`, `funktor/core`, `kraft/core`, `kraft/addons/datetime`
- treat it as its own commit so it can be reverted independently

## STEP 3 — the concrete win: drop `com.benasher44:uuid`

`kotlin.uuid.Uuid` became **Stable** in 2.4.0, which makes the third-party dependency
(`Deps.kt:65-67`, version 0.8.4) removable. Exactly two usages, both `uuid4()`:

- `funktor/auth/src/jsMain/kotlin/widgets/GoogleSignInButton.kt`
- `funktor/core/src/commonMain/kotlin/websocket/WsClientMessage.kt`

Replacement is `Uuid.random()`. **Check first** whether `Uuid.random()` needs an opt-in — the V4/V7
*generator* functions stayed Experimental in 2.4.0 even though the core API went Stable.

- [ ] migrate both call sites
- [ ] remove the dependency and its `Deps` entry

## STEP 4 — Java-side dependencies

Independent of Kotlin, worth doing in the same pass since the full suite has to run either way.
Current versions:

| Dep | Current |
|---|---|
| Jackson | 2.21.1 (annotations 2.21) |
| MongoDB driver BOM | 5.6.4 |
| ArangoDB java driver | 7.25.0 |
| AWS SDK | 2.42.8 |
| jakarta.mail | 1.6.8 |
| snakeyaml | 2.4 |
| google-api-client | 2.9.0 |
| firebase-admin | 9.8.0 |
| auto-service | 1.1.1 |
| clikt | 5.1.0 |
| kotlinpoet | 2.2.0 |
| kotlin-csv | 1.10.0 |
| kotlin-faker | 1.16.1 |
| qrcode-kotlin | 4.5.0 |
| excelkt | 1.0.2 |

The two database drivers are the risky ones — they are exercised by real-DB e2e suites, so a
regression there shows up as a test failure rather than a compile error. Bump them separately from
the Kotlin change so a bisect is possible.

## STEP 5 — verify

- [ ] `./gradlew allTests --continue` green on **every** module, compared against the STEP 0 baseline
- [ ] Counts read from the XML, per module — a module that silently drops to 0 tests is a regression
- [ ] Both DB backends exercised (`docker start mongodb arangodb`)
- [ ] JS and native targets included, not just JVM
- [ ] `docs-site` versions (`docs-site/src/data/site.ts`) and the README dependency snippet
      (`README.MD` ~line 123) only matter if `VERSION_NAME` changes — this task does not bump it

## Sequencing advice

Land it in separate commits so a bisect can find the culprit:

1. baseline recorded (no code change)
2. Kotlin + KSP + Dokka
3. kotlinx set + Ktor
4. `kotlin.uuid` migration and `benasher44` removal
5. Java-side deps

## Deferred until the stdlib APIs stabilise

Not part of this task, but recheck when doing it — these are still Experimental as of 2.4.x, which is
why `ultra/common` keeps its own versions:

- `kotlin.io.encoding.Base64` (`@ExperimentalEncodingApi`) vs `toBase64`/`fromBase64`
- `ByteArray.toHexString()` (`@ExperimentalStdlibApi`) vs `toHex`

Both produce byte-identical output to ours (verified by probe on 2.3.10). Adopting them today would
force an `@OptIn` onto every downstream consumer. Note `fromBase64` is not a straight swap regardless:
ours tolerates missing padding, `Base64.decode` throws on it.

Also worth a look then: `getOrPutIfMissing` (new in 2.4.0, Experimental) distinguishes a stored `null`
from an absent key — the exact problem `NullableCache` solves by hand with a `MISSING` sentinel.
