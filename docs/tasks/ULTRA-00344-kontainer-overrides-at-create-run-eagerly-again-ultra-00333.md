# ULTRA-00344: Kontainer: overrides at create run eagerly again (ULTRA-00333 reverted)

> **Status (2026-11-14): DESIGNED.** Decided by the maintainer; track D builds it.
>
> **Part of:** ULTRA-00255 (Epic: Ultra libraries (module scans and defects))
> **Plan:** ULTRA-00316 (close the open epics from the foundation upwards)

## Why this exists

ULTRA-00333 (batch K1, merged `f575dd29`, not released) made the lambda of `create { with { X() } }` lazy: it ran
at the first request of the service from that kontainer. Discussing what should happen when such a lambda throws
showed the cost: a failure moves from `create` to whichever service first injects the override, on whichever
thread; a retry after a throw can heal, but can also leave one service that saw the failure next to another that
got a value; a clone runs its parent's lambdas later and concurrently; the kontainer keeps lambdas and a lock per
override. Every override in the repo is a ready value or a cheap wrapper (`with { call.currentUserProvider() }`
builds a `UserProvider.lazy { }`), so laziness saved no work.

## Decided (maintainer, 2026-11-14)

"Ok then back to eager and if the user need laziness it can be built in place, like UserProvider.lazy {} ..."

- The lambda of `create { with { ... } }` is a factory that runs exactly once per kontainer, at `create`.
- A throwing override fails `create`: a kontainer exists completely or not at all. No retry, nothing remembered.
- `clone()` runs the lambdas again for the clone, as before K1, so a clone has its own instances.
- A caller who needs laziness builds it in place, as `UserProvider.lazy { }` does.

## The work

- Revert the laziness of ULTRA-00333 only: `ServiceProviderFactory` runs the override at create;
  `ServiceProvider.ForOverride` holds the instance again; the provider lock and the "until it has returned a
  value" state go.
- Everything else of K1 stays: the redefinition rule (ULTRA-00331), `extend` (ULTRA-00330), `clone` checking like
  `create` through the one entry point (ULTRA-00332), the built-ins (ULTRA-00336), the checker keying its cache
  with a copy (ULTRA-00337), and the K2 order: validation before the override block.
- Tests: the eager behaviour pinned as decided (not as "CURRENT BEHAVIOUR"); the lazy specs removed or turned.
- `CHANGELOG.md` `## Unreleased`: the ULTRA-00333 entry goes, since eager is what the last release did; what
  remains different from the last release is said under the tickets that cause it.

## Acceptance

- `create { with { X() } }` runs the lambda at `create`, once; `clone()` runs it once more for the clone; a
  throwing lambda fails `create` or `clone`.
- Kontainer and the suites that build a blueprint are green; the gate is closed.

## History

- 2026-11-14: created on the maintainer's decision, after the discussion of the retry in ULTRA-00333.
