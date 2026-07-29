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

Save the baseline somewhere durable before proceeding.

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

`kotlinx-datetime` is pre-1.0 and has had breaking changes between minors — read its changelog rather
than bumping blind. `ultra/datetime` wraps it heavily.

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
