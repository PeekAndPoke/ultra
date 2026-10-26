# ULTRA-00241: Streams: filterIsInstanceOf(KClass), one call without type arguments

> **Status (2026-10-26): DONE.** gate PASS in round 2; filterIsInstanceOf on master, unreleased

**Security-critical:** no

## Why this exists

`inline fun <reified I : T, T> Stream<T>.filterIsInstance()` keeps the subtype check (`I : T`), but Kotlin wants
every type argument once one is given, so a call reads `filterIsInstance<Diagnostics, _>()`. Klang hit it moving two
Motor components to `switchMapNotNull` (klang `74c17d14`).

## Decided

- Checked with kotlinc 2.1.10, 2026-10-26: no form with one type argument keeps the check as a plain extension
  ("2 type arguments expected"); a member on `Stream` cannot be inline (an interface member is virtual). A `KClass`
  argument infers both and keeps the check (`Other::class` is a type mismatch).
- **Not an overload of `filterIsInstance`:** on a `Stream<Any?>` (or `Stream<Any>`) `filterIsInstance(B::class)`
  is ambiguous with `filterIsInstance(initial: I)`, since a `KClass` is an `Any` (kotlinc: "overload resolution
  ambiguity"). The maintainer chose the name `filterIsInstanceOf`.
- Both shapes, mirroring `filterIsInstance`: `filterIsInstanceOf(cls): Stream<I?>` and
  `filterIsInstanceOf(cls, initial): Stream<I>`. Bound `I : T & Any`, so `KClass<I>` is legal on nullable streams.
- Non-inline: `cls.isInstance(it)`, common stdlib, all three platforms.

## The work

- [x] `ultra/streams/src/commonMain/kotlin/ops/filter.kt`: the two functions.
- [x] `ultra/streams/src/commonTest/kotlin/ops/FilterIsInstanceOfSpec.kt`: 4 tests (instances only and null before
  the first; `initial`; a supertype and an interface, each runtime's own `isInstance`; a `Stream<Any?>`).
- [x] Docs: `docs-site/src/pages/ultra/streams/operators.astro` and `docs-site/src/data/llms/streams.md`. Their
  `filterIsInstance<String>()` examples did not compile (one type argument); now both forms, with a subscription.
- [x] CHANGELOG `## Unreleased`.

## Acceptance

- [x] `stream.filterIsInstanceOf(B::class)` compiles without type arguments on `Stream<Data?>` and `Stream<Any?>`.
- [x] A class outside the stream's type does not compile (kotlinc, above).
- [x] Tests green on JVM, JS (browser) and linuxX64.

## Test evidence

- `./gradlew :ultra:streams:jvmTest :ultra:streams:jsTest :ultra:streams:linuxX64Test`, 2026-10-26: green.
  `FilterIsInstanceOfSpec` 3/3 on each, 4/4 on each after round 2's test (`.build-claude/ultra/streams/test-results/*/TEST-*FilterIsInstanceOfSpec.xml`),
  `FilterSpec` 5/5 (JS), native 69/69.
- Docs site build: 114 pages, `filterIsInstanceOf` in the operators page and the streams LLM mirror.
- No mutation check: neither security-relevant nor persistence (CLAUDE.md scope).

## Review record

| Round | Level | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|---|
| 1 | 1 | MAJOR | `operators.astro:121-123` | Docs say `filterIsInstance(String::class)` on `Stream<Any?>` does not compile; it compiles as the `initial` overload (`Stream<KClass<String>>`) and silently filters for KClass values | fix: the page warns never to pass a class to `filterIsInstance`; one-line warning in the mirror | Confirmed with kotlinc 2.1.10: `val x: Stream<KClass<String>> = any.filterIsInstance(String::class)` compiles; on `Stream<Signal?>` a type mismatch. The ambiguity in Decided applies only to the overload that was not added |
| 1 | 1 | MINOR | `CHANGELOG.md:9-11` | The name's reason reads as today's behaviour ("would not compile") | fix: "as an overload it would be ambiguous", plus the trap | Same fact as the MAJOR |
| 1 | 1 | MINOR | `operators.astro:119-120` | "both reified": only `I` is | fix: "as type arguments (`<String, _>`)" | `filter.kt:36,44` |

Round 1 (one `reviewer-high`, implementation & code style with API fit; scoped by risk: no security or domain
charter). Clean in round 1: behaviour against `StreamWrapperBase` (the docs example prints "text" once on subscribe),
the three test traces, the subtype check (`Stream` is invariant), KDoc links, imports, `cls.isInstance` on all
platforms, the mirror. SURPLUS: `filterIsInstanceOf(cls, initial)` has no caller yet; kept for parity with
`filterIsInstance(initial)` (maintainer may drop it). The KDoc's naming note was corrected along with the MAJOR.

| Round | Level | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|---|
| 2 | 2 | MINOR | `FilterIsInstanceOfSpec.kt:17-64` | No test passes a subclass or an interface implementation; `it::class == cls` would survive | fix: test "lets subclasses and implementations through" | Green on JVM, JS, linuxX64 (4/4) |
| 2 | 2 | MINOR | `operators.astro:77-115`, `getting-started.astro:104`, `streams.md:185/291/294`, `filter.kt:17` | Pre-existing: the `filter` examples read an unsubscribed operator and print values it never has (it keeps `latest` only while subscribed); `filterNotNull`'s prose says `Stream<String?>`; `filter(initial)`'s KDoc copied from `filterNotNull` | out-of-scope: listed for the maintainer | Outside this change's section; a docs pass of its own |

Round 2 (one `reviewer-xhigh`, level 2 after round 1's MAJOR; two-phase): 0 CRITICAL, 0 MAJOR. Phase 2: no overlap
with settled entries; all three round-1 fixes hold in the page, mirror, CHANGELOG and KDoc; no rejections to audit.
Disclosed: the reviewer's Read showed it the ledger before phase 1; it checked every point against the files.

**Verdict:** gate PASS (round 2 clean).

## History

- 2026-10-26: created.
- 2026-10-26: implemented; tests green on three platforms. Next: /feature-review.
- 2026-10-26: review round 1 (1 MAJOR, 2 MINOR, fixed), round 2 clean; gate PASS. DONE. Docs were updated in the change
  itself, so no DOCS follow-up.
- 2026-10-26: DONE. gate PASS in round 2; filterIsInstanceOf on master, unreleased
