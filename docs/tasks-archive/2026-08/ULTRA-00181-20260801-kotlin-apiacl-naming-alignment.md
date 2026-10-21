# ULTRA-00181: Align the Kotlin `ApiAcl` vocabulary with the TypeScript SDK

**Status:** DONE (archived 2026-10-19) — implemented 2026-08-01 alongside the TypeScript side; one implementation
reviewer (maintainer, 2026-10-19), its MAJOR and MINORs fixed. Docs were updated in the same change, so no docs
follow-up task. One design question parked for the maintainer (below).
**Plan:** none — a standalone rename that falls out of
ULTRA-00182 (sdk route access control)
**Security-critical:** no — a rename of an advisory client-side lookup; nothing here enforces
anything, and no call site changes meaning

## Why

The TypeScript SDK is gaining a route-access API, and the vocabulary settled on 2026-08-01 is better
than the one Kotlin has. Left alone, the two SDKs would carry **inverted semantics under similar
names**, which is a genuine trap for anyone working across both:

| | Kotlin today | means |
|---|---|---|
| `hasAccessTo` | `level.isGranted()` | strict — `Granted` only |
| `hasAnyAccessTo` | `!level.isDenied()` | permissive — `Granted` or `Partial` |

The new TS `canAccess` is the **permissive** one. A reader who assumes `canAccess` ≈ `hasAccessTo`
gets it exactly backwards.

`hasAnyAccessTo` is also the vaguest name of the set — "any access" does not tell you that the
server will still check your arguments.

## The target

`ApiAcl` (`funktor/rest/src/commonMain/kotlin/acl/ApiAcl.kt`):

| new | old | level |
|---|---|---|
| `canAccess` | `hasAnyAccessTo` | **not** `Denied` |
| `canFullyAccess` | `hasAccessTo` | `Granted` |
| `canPartiallyAccess` | *(new)* | `Partial` |
| `isDenied` | *(new)* | `Denied` |

`getAccessLevel` keeps its name.

**`ApiAccessLevel`'s own predicates stay exactly as they are.** `isGranted()` / `isPartial()` /
`isDenied()` (`ultra/remote/src/commonMain/kotlin/ApiAccessLevel.kt`) are exact-match tests on the
enum, they are correctly named, and they have real production callers — `AuthRule.kt:269`,
`ApiRoute.kt:108`, `ApiAccessDescriptor.kt:30`. This task does not touch them. Note the resulting
`ApiAcl.isDenied(endpoint)` vs `ApiAccessLevel.isDenied()` pair: different receivers, no conflict,
and both read correctly at their call sites.

## Blast radius: tests only

`hasAccessTo` and `hasAnyAccessTo` have **no production callers**. Verified by grep across the repo
(excluding `build/`): every use is in `funktor/rest/src/jvmTest/kotlin/acl/ApiAclSpec.kt`. `ApiAcl`
itself appears elsewhere only in KDoc prose (`UserApiAccessMatrix.kt:10`,
`funktor/auth/src/jsMain/kotlin/AuthSessionConfig.kt:23`).

**No deprecated aliases.** This repo does not carry back-compat shims. A clean rename is correct —
but see the trap below before assuming it is risk-free.

## The one trap

**Nothing is currently named `canAccess`, and that is what makes this safe.** If it existed with the
strict meaning, the rename would silently invert every call site while still compiling. It does not,
so the compiler enumerates every caller.

Do not "helpfully" add a deprecated `hasAccessTo` alias pointing at the new `canAccess` — that would
manufacture exactly the inversion this task exists to prevent. If an alias is ever wanted, it must
point at `canFullyAccess`.

## Spec

- [x] Rename the two methods and add the two new ones, with KDoc stating the invariants:
      `canAccess` ≡ `!isDenied`; the three exact-match predicates are mutually exclusive and
      exhaustive; `canAccess` ≡ `canFullyAccess || canPartiallyAccess`.
- [x] KDoc on `canAccess` must say it includes `Partial`, i.e. the server may still reject specific
      arguments — and that it is the predicate for visibility, destructive actions INCLUDED (corrected by `56a896f9`:
      the first wording sent destructive actions to `canFullyAccess`, which hides them on the user's own resource).
- [x] KDoc on `canPartiallyAccess` must say what `Partial` means: the server checks the ARGUMENTS,
      typically that an id in the path is the caller's own. The matrix cannot evaluate that; the
      caller must apply the equivalent rule.
- [x] Update `ApiAclSpec.kt`, and extend it to cover the two new predicates against all three levels.
- [x] Update the `ApiAcl` class-level KDoc usage example (it currently shows `hasAccessTo`).

## Test evidence

- [x] `ApiAclSpec` covers all four predicates against `Granted` / `Partial` / `Denied` **and** the
      absent-entry case, which must read `Denied` (the absent case pinned only three of them until the 2026-10-19
      review).
- [x] Mutation-test: inverting any predicate must fail a test. The existing spec asserted only two
      of the three levels per predicate — worth checking it is not vacuous while renaming it.
- [x] Compile sweep, since this is an ABI change to a published module:
      `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs
      compileKotlin compileTestKotlin --continue` and check for `^e:`.
- [x] Full test command(s) run + green: `./gradlew :funktor:rest:jvmTest` — 111 tests, 0 failures.
      Compile sweep clean. Grep confirms no remaining caller of either old name.

## Review record (filled by /feature-review)

Scope (maintainer, 2026-10-19): ONE implementation reviewer, then archive. Domain and security charters not run — the
change is a rename of an advisory, client-side lookup.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `funktor/rest/src/commonMain/kotlin/acl/ApiAcl.kt:71-76` (+ `auth.astro:261-265`, `llms/funktor.md:300-303`, TS `runtime/acl.ts:117-120`) | `canFullyAccess` KDoc promises "callable unconditionally … regardless of WHICH resource"; `forCall` estimates `Granted` by default, so an ownership-checked route reads full access | CONFIRMED → fixed | verified `AuthRule.kt:51`, `AuthRuleBuilder.kt:84`: `estimateFn` defaults to `Granted`. All four surfaces now say `Granted` = no rule REPORTED an argument check |
| 1 | MINOR | `ApiAcl.kt:88-91` | the `canPartiallyAccess` example's `canFullyAccess ||` arm admits everyone on a default-estimate `forCall` route | CONFIRMED → fixed | KDoc states the route's rule must estimate `Partial` |
| 1 | MINOR | `ApiAclSpec.kt:98-106` | absent entry pinned `canAccess`/`isDenied` only, not `canFullyAccess`/`canPartiallyAccess` | CONFIRMED → fixed | two assertions added |
| 1 | MINOR | `ApiAcl.kt:97` | `isDenied` "Never callable." — false for unlisted routes, e.g. `public()` under `ApiAcl.empty` | CONFIRMED → fixed | Kotlin + TS reworded |
| 1 | MINOR | this file `:71-72`, `:81-82` | spec line carried the pre-`56a896f9` advice; test-evidence tick overstated | CONFIRMED → fixed | rewritten above |
| 1 | — | docs `Partial` sentence | "only a hand-written `AccessLevelCheck` emits `Partial`" — `forCall(estimateFn = { Partial })` does too | found by the coordinator while fixing → fixed | docs + TS reworded |

Probed and CLEAN (reviewer): predicate mapping; the grid test kills inversion/swap mutants; "server omits Denied
entries" (`ApiAccessDescriptor.kt:29-31`); no built-in rule yields `Partial`; no old names outside `.claude/`; imports,
KDoc links, branding; `ApiAccessLevel` predicates untouched.

Tests after fixes: `:funktor:rest:jvmTest` 116/0, `:ultra:codegen:test` 325/0 (XML fresh, `ApiAclSpec`,
`AclRuntimeSpec` ran).

**Parked for the maintainer:** should `forCall` default its estimate to `Partial` instead of `Granted`? Its `checkFn`
sees params and body, so it is argument-dependent by construction; `Partial` would make the matrix honest for
ownership checks. `canAccess` is unaffected either way (both levels are access); only `canFullyAccess` /
`canPartiallyAccess` change, for every app route using `forCall` without an `estimateFn`. A user-only `forCall` (e.g.
"is admin") would then under-report as `Partial`. Behaviour change → not made here.

**Verdict:** PASS (no open CRITICAL/MAJOR).

## History

- 2026-10-21: numbered ULTRA-00181 (was `docs/tasks-archive/2026-08/20260801-kotlin-apiacl-naming-alignment.md`).
