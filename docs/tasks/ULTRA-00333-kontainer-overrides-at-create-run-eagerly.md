# ULTRA-00333: Kontainer: overrides at create run eagerly

> **Status (2026-11-14): NOT STARTED.** 
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))

**Found by:** the characterization specs of ULTRA-00319 (Kontainer injection type split), step 1, 2026-11-14.

## Why this exists

The lambda in `create { with { X() } }` runs at create, even when `X` is never requested from that kontainer; `clone()` runs it again.

Pinned as it is today in `ultra/kontainer/src/test/kotlin/e2e/characterization/DynamicsAtCreateCharacterizationSpec.kt`, in a test
whose name starts with "CURRENT BEHAVIOUR". The split of ULTRA-00319 keeps this behaviour unchanged; the fix is this
ticket (maintainer, 2026-11-14: "fix each with its own ticket").

## Decided (maintainer, 2026-11-14)

"yes make override lazy": the lambda of `create { with { ... } }` runs the first time the service is requested from
that kontainer, and its result is kept for that kontainer (at most one run per kontainer). Today it runs at `create`
(`ServiceProviderFactory.kt:22`), and again at `clone()`.

Behaviour changes without a compile signal, for the changelog: an override that throws now throws at the first `get`;
side effects run later, or never when the service is not used; a lambda reading something mutable sees the value at
first use; `clone()` no longer runs the lambda at once. Build it together with ULTRA-00337 (the dynamics checker keeps
the first request's override lambdas alive) and ULTRA-00332 (one entry point for `create` and `clone`).

## Plan as built (track D, batch K1, 2026-11-14)

- **Laziness:**
  - `ServiceProviderFactory` no longer calls the override lambda; `ServiceProvider.ForOverride` holds it.
  - The first `provide` runs it under the provider's lock, keeps the value and drops the lambda, so it runs at most
    once per kontainer.
  - The base-type lookup of an override (`ServiceAmbiguous` for an ambiguous one) stays at create.
- **`clone()`:** does not run any lambda. The clone gets its own lazy cells from the same lambdas, as it gets fresh
  instances today.
- **The kontainer still holds its override lambdas,** in `ServiceProviderFactory.dynamics`, because `clone` needs
  them. "After first use only the value is kept" holds for the provider. A collected kontainer leaves nothing
  reachable from the blueprint (proven in ULTRA-00337).
- **Consequences without a compile signal, beyond the ticket's list:**
  - The definition of an override can no longer name the instance's class before it exists. `creates` is the class
    given to `with` (the same as today for `with { X() }`, the base class for `with(Base::class) { Derived() }`).
  - An override that was never requested has no instance. It reports none in `instances`, the dump and debug info,
    and it no longer appears in the insights record slice, which lists only services with instances.
- **Callers whose lambda now runs later:**
  - `with { call.currentUserProvider() }` (funktor-demo, the `funktor:all` test server): it only builds a
    `UserProvider.lazy { }`, so nothing observable moves.
  - `with { UserProvider.system() }`, `with { NullQueryProfiler }` (`AppKontainers.system()`) and
    `with { kronos }` (`WorkersFacade`): values, no side effect.
  - Test fixtures in `funktor/cluster` and `funktor/insights`: listed in the track report.

## Built (batch K1, 2026-11-14)

As planned, with one refinement: once an override has run, its definition names the instance's class and keeps the
code location of `create`, exactly as before. Only an override that was never requested differs: its definition
names the class given to `with`.

**Open point:** "after first use only the value is kept, not the lambda" holds for the provider, and a test pins that.
The kontainer itself still holds its override lambdas for its lifetime, because `clone` re-runs them for the clone's
own instances. A collected kontainer leaves nothing reachable (ULTRA-00337 tests). If the maintainer wants the
kontainer to drop them too, `clone` would have to change meaning.

Pins turned, before -> after:
- `DynamicsAtCreateCharacterizationSpec`, "CURRENT BEHAVIOUR: the override lambda runs at create ..." -> "The
  override lambda runs at the first request of the service, once per kontainer, never at create":
  - runs after create: 1 -> 0
  - after two gets: 1 -> 1
  - after a second `create`: 2 -> 1
- `DynamicsAtCreateCharacterizationSpec`, "Cloning keeps the overrides and runs their lambdas again": runs right
  after `clone()`: 2 -> 0; after a get in each kontainer: 2.
- `ToolsCharacterizationSpec`: one clue text, "created at create" -> "created at its first request".
- `ScopeGuardsSpec`: `forOverride(definition, instance = ...)` -> `forOverride(definition, create = { ... })`, the
  new signature.
- `funktor/insights` `KontainerWireNamesSpec`: the end-to-end wire test now also requests the overridden service.
  Without that, its never-requested override has no instance and is missing from the slice, the consequence named
  above. A new test there pins that consequence.
- New tests:
  - a never-requested override never runs and has no instance
  - a throwing override fails at the first get with its own exception
  - mutable state is read at first use
  - the definition's class before and after first use
  - the provider keeps only the value

Mutation (failed / 524 or 525):

| Mutant | Result |
|---|---|
| KL1: evaluated eagerly at create | RED, 5 |
| KL2: evaluated on every request | RED, 7 |
| KL3: the lambda kept after use | SURVIVED at first; RED, 1 after the provider test was added |

Changelog (behaviour change, no compile signal): `ultra/kontainer`: overrides at `create { with { ... } }` are lazy.
The lambda runs at the first request of that service from that kontainer, at most once per kontainer; `clone()` no
longer runs it at once. Consequences:
- An override that throws now throws at the first `get`, and again at the next, as no value was kept.
- Side effects run later, or never when the service is not used.
- A lambda reading something mutable sees the value at first use.
- An override never requested has no instance: none in `instances`, the debug info or the dump, and none in the
  insights record slice.
- Until its first use, its definition names the class given to `with` (ULTRA-00333).

Callers checked, nothing observable moves:
- `with { call.currentUserProvider() }` only builds `UserProvider.lazy { }`
- `with { UserProvider.system() }`, `with { NullQueryProfiler }` and `with { kronos }` are values

Commits:
- `9fe4095b`: the plans
- `a4977d16`: today's built-in behaviour pinned
- `dc73ee47`: tests first and the fix, 115 tests red before it and all green after
- `55ff797f`, `cf5358fc`, `d6ed8dd9`: follow-ups

Suites after K1 (XML, `--rerun`, 0 failures):

| Suite | Tests |
|---|---|
| `:ultra:kontainer:test` | 525 (420 before K1) |
| `:ultra:log:jvmTest` | 74 (no log file touched) |
| `:ultra:vault:jvmTest` | 287 |
| `:funktor:insights:jvmTest` | 118 (117 before) |
| `:funktor:all:jvmTest` | 289 |

Repo-wide compile sweep: green, 459 tasks, 0 errors.

## The work

- Decide: keep eager (simple, an override is a value) and document it, or make it lazy. If it stays, the KDoc of `with` says so.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
