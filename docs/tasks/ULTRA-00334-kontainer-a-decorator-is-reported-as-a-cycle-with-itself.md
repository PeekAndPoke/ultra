# ULTRA-00334: Kontainer: a decorator is reported as a cycle with itself

> **Status (2026-11-14): BUILT** on `wt/kontainer`, batch K2, not merged; a strict gate follows.
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

Changelog (behaviour change, message): `ultra/kontainer`: a single-service injection whose type has several
candidates, one of them the service itself, is reported as ambiguous ("Parameter 'inner' is ambiguous. The following
services collide: ..."). Before, it was reported as "Circular dependency detected: CachingRepo -> CachingRepo". The
typical case is a decorator, `CachingRepo(inner: Repo) : Repo`. Which service is injected does not change; a
decorator names the concrete class (ULTRA-00334).

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

## The work

- See "Decided": the diagnosis changes, the resolution does not.
- Tests first: the "CURRENT BEHAVIOUR" pin turns into the wanted expectation (RED before the fix); mutation check.
- CHANGELOG: a behaviour change without a compile signal.

## Acceptance

- The pin no longer says "CURRENT BEHAVIOUR"; `:ultra:kontainer:test` and the consumer suites are green.

## History

- 2026-11-14: created on the maintainer's word.
