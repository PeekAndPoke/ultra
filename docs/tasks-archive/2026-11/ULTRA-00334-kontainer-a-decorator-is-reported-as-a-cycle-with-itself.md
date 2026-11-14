# ULTRA-00334: Kontainer: a decorator is reported as a cycle with itself

> **Status (2026-11-14): DONE.** Batch K2, merged into master as `8778c4a9`: strict gate round 1 CLEAN. Behaviour changed; the docs follow in ULTRA-00338 (kontainer docs).
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

A decorator with a nullable parameter of its own super type, `DecoratingRepo(inner: Repo?) : Repo`, is reported by validation as a cycle with itself ("DecoratingRepo -> DecoratingRepo").

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/ValidationCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

The maintainer rejected a new resolution rule (leaving a service out of the candidates for its own parameter): "Should
fail due ambiguity"; "Yes fix the error message, no new refusal rule and no new mechanics ... much simpler".

Why the message is wrong today: an injection of `Repo` has every registered service that is a `Repo` as a candidate
(`TypeLookup.ForSuperTypes`, `isAssignableFrom`). In `class RealRepo : Repo()`, `class CachingRepo(val inner: Repo) :
Repo()` that is two, one of them the owner. The dependency graph gets an edge to each candidate, the cycle check runs
first, sees `CachingRepo -> CachingRepo` and stops before the ambiguity check.

So, diagnosis only:

- A single-service injection (plain `T`, `Lazy<T>`) with several candidates is reported as ambiguous, with the existing
  message naming the colliding services.
- The cycle check follows a single-service injection only when it resolves to exactly one service.
- A real self-cycle stays a cycle: when the owner is the only candidate, "circular dependency" is right.
- No change to which service is injected. A decorator names the concrete class: `CachingRepo(val inner: RealRepo)`.

Built with ULTRA-00328 (the cycle check reads the parameter providers' announcements). The nullable variant
(`inner: Repo?` with several candidates injects null) is ULTRA-00335.

## Plan as built (track D, batch K2, 2026-11-14)

- In the cycle check (ULTRA-00328), a Direct parameter that announces exactly one type with two or more candidates
  is an ambiguous single-service injection. It gets no cycle edges, and the parameter check reports it with the
  existing "is ambiguous" message.
  - A `List` never falls under this: it announces every candidate.
  - A real self-cycle (the owner is the only candidate) stays a cycle.
- No change to which service is injected, and no new refusal rule.
- The nullable decorator (`inner: Repo?`) becomes ambiguous through ULTRA-00335.

## Built (batch K2, 2026-11-14)

**Status: built on `wt/kontainer`, not merged; a strict gate follows.**

As planned: a Direct parameter that announces one type with several candidates gets no cycle edges, so the
parameter check reports it as ambiguous. No change to which service is injected; no new rule.

Pins turned, before -> after:
- "CURRENT BEHAVIOUR: a decorator with a nullable parameter of its own super type is reported as a cycle":
  `Circular dependency detected: DecoratingRepo -> DecoratingRepo` -> `Parameter 'inner' is ambiguous. The following
  services collide: RealRepo, DecoratingRepo` (with ULTRA-00335).
- "CURRENT BEHAVIOUR: a decorator with a non-null parameter ..." (pinned first in `8dffdf22`):
  `CachingRepo -> CachingRepo` -> `Parameter 'inner' is ambiguous. ... RealRepo, CachingRepo`.
- New:
  - a real self-cycle, where the owner is the only candidate, stays a cycle
  - a decorator naming the concrete class (`ConcreteCachingRepo(inner: RealRepo)`) is valid and gets it injected

Mutation:

| Mutant | Result |
|---|---|
| A1: ambiguous single-service injections followed by the cycle check again | RED, 2 / 555 |
| A2: any parameter with several candidates left out (would drop List cycles) | SURVIVED at first; RED, 1 / 556 after the two-candidate List cycle test |

Changelog (behaviour change, message): `ultra/kontainer`: every single-service injection (`T`, `T?`, `Lazy<T>`)
whose type has several candidates is reported as ambiguous ("Parameter 'i' is ambiguous. The following services
collide: ..."), also when a dependency cycle went through it. Examples:
- A decorator, `CachingRepo(inner: Repo) : Repo`: was "Circular dependency detected: CachingRepo -> CachingRepo".
- `A(i: I)`, `B(a: A) : I`, `C : I`: was "Circular dependency detected: A -> B -> A".

Other parameter errors that the cycle error used to hide now show as well. Both versions refuse the blueprint; only
the message changes. Which service is injected does not change; a decorator names the concrete class (ULTRA-00334).

Commits:
- `8dffdf22`: today's pins
- `4b404195`: the plans
- `01cbd47d`: the fix, with tests first (8 red before it, all green after)
- `2281c29d`: a `List` cycle with two candidates, added for mutant A2

Suites (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 556 |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |

The repo-wide compile sweep is green (459 tasks, 0 errors). No blueprint in the repo turned red.

## K2 gate round 1 (strict, blind, reviewer-fable-xhigh)

The same ledger is written into ULTRA-00327, 00328, 00334 and 00335.

**Verdicts:** implementation CLEAN (6 MINOR); domain and security CLEAN (6 MINOR); no CRITICAL, no MAJOR. Neither
reviewer ran anything. The close-out has no production change (tests and texts only; commits `54b8e8c1`,
`a9ccf8c0`, plus the ticket commit).

**Found CLEAN:**
- Both reviewers traced all seven parameter providers against master: kinds and outcomes are unchanged outside
  rules 1 to 4.
- Rule 3 (an ambiguous single-service injection gets no cycle edges) cannot hide a real cycle.
- The nullable survey of seven is exact; funktor-demo adds none.
- There is no `List` cycle in the repo.

### Findings and outcomes

1. **The "exactly one announced type" conjunct was unpinned.** New test: `PartOwner(all: List<Part>)` with
   `MidPart(owner)` and its subclass `LeafPart`, all registered, is a cycle. Mutant D1 ("ambiguous when any announced
   type has several candidates"): RED, 1 / 561.
2. **The composite in its own list.** New tests:
   - `CompositeHandler(all: List<HandlerBase>) : HandlerBase` is `Circular dependency detected: CompositeHandler ->
     CompositeHandler`.
   - Characterization row: with `Lazy<List<HandlerBase>>` it boots, and the list contains the composite itself.
   - Mutant D2 (the owner left out of its own edges): RED, 2 / 561.
3. **The guard in `createChecked`.** New test through the public `ServiceProviderFactory(...)` constructor and
   `newKontainer` on an invalid blueprint that never ran `create`: `KontainerInconsistent`. Mutant D3 (guard deleted):
   RED, 1 / 561.
4. **Rule 2 for the other forms.** One test: a `List` cycle is found when the owner is a factory, a dynamic or a
   prototype. Mutant L2 (cycle check skips `List`), re-run: RED, 5 / 561.
5. **`Lazy<Absent?>` with no candidate is null.** Added to the existing test. Mutant D5 (that `ForLazyService` arm
   turned into an error): RED, 2 / 561.
6. **Literal class names** in the expectations K2 added (`ValidationCharacterizationSpec`,
   `InjectionFormsCharacterizationSpec`); master's older ones are left.
7. **Evidence.** The other suites that build a blueprint were run: `funktor:core`, `rest`, `auth`, `messaging`,
   `logging`, `codegen`, `saas`, and `funktor-demo:server` (it has a `test` task). All green; counts below.
8. **ULTRA-00334 changelog** is worded for the general case, with the `A(i: I)`, `B(a: A) : I`, `C : I` example and
   the parameter errors the cycle used to hide. Both are pinned by a new test, which mutant A1 kills as well (RED,
   3 / 562).
9. **ULTRA-00328 changelog:** it can stop an app at boot; the composite in its own list; `Lazy<List<T>>` only defers
   the cycle; the `Lazy<T>` hint helps only when `.value` is read after construction.
10. **ULTRA-00328 "Built":** "all other promotion and injection-form characterization tests passed untouched"; the
    one that changed is the ambiguous nullable pin, turned by ULTRA-00335.
11. **ULTRA-00335 "Built":** the `Lazy<T?>` claim with no candidate is now pinned (item 5); mutant recorded.
12. **ULTRA-00327 mutation table:** "validate on every create instead of remembering" is recorded as equivalent next
    to Q3. `validate()` tracks its throwaway kontainer, which is not observable from outside.
13. **This ledger and the status line.**

### Counts after round 1 (XML, `--rerun`, 0 failures)

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 562 |
| `:ultra:log:jvmTest` | 74 |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 |
| `:funktor:cluster:jvmTest` | 162 |
| `:funktor:all:jvmTest` | 289 |
| `:funktor:core:jvmTest` | 775 |
| `:funktor:rest:jvmTest` | 124 |
| `:funktor:auth:jvmTest` | 322 |
| `:funktor:messaging:jvmTest` | 67 |
| `:funktor:logging:jvmTest` | 14 |
| `:funktor:codegen:test` | 104 |
| `:funktor:saas:jvmTest` | 50 |
| `:funktor-demo:server:test` | 52 |

The kontainer suite had 561 tests in the run of all fourteen suites; it went to 562 with the item 8 test, re-run
alone. No blueprint in the repo turned red in any of these suites.

## The work

- See "Decided": the diagnosis changes, the resolution does not.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
- 2026-11-14: DONE.
