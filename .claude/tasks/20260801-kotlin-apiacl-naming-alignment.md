# Align the Kotlin `ApiAcl` vocabulary with the TypeScript SDK

**Status:** IN REVIEW — implemented 2026-08-01 alongside the TypeScript side
**Plan:** none — a standalone rename that falls out of
`.claude/tasks/20260801-sdk-route-access-control.md`
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
      arguments, and that destructive actions should use `canFullyAccess`.
- [x] KDoc on `canPartiallyAccess` must say what `Partial` means: the server checks the ARGUMENTS,
      typically that an id in the path is the caller's own. The matrix cannot evaluate that; the
      caller must apply the equivalent rule.
- [x] Update `ApiAclSpec.kt`, and extend it to cover the two new predicates against all three levels.
- [x] Update the `ApiAcl` class-level KDoc usage example (it currently shows `hasAccessTo`).

## Test evidence

- [x] `ApiAclSpec` covers all four predicates against `Granted` / `Partial` / `Denied` **and** the
      absent-entry case, which must read `Denied`.
- [x] Mutation-test: inverting any predicate must fail a test. The existing spec asserted only two
      of the three levels per predicate — worth checking it is not vacuous while renaming it.
- [x] Compile sweep, since this is an ABI change to a published module:
      `./gradlew compileKotlinJvm compileTestKotlinJvm compileKotlinJs compileTestKotlinJs
      compileKotlin compileTestKotlin --continue` and check for `^e:`.
- [x] Full test command(s) run + green: `./gradlew :funktor:rest:jvmTest` — 111 tests, 0 failures.
      Compile sweep clean. Grep confirms no remaining caller of either old name.

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...
