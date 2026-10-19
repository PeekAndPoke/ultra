# `forCall` estimates `Partial` by default

**Status:** TODO — maintainer decision 2026-10-19 (relayed by Mission-Control): "yes, Partial"
**Plan:** none — follow-up of the apiacl review (`.claude/tasks-archive/2026-08/20260801-kotlin-apiacl-naming-alignment.md`,
"Parked for the maintainer")
**Security-critical:** no — the access matrix is advisory; the server still enforces every rule

## Why

`forCall`'s `checkFn` sees the request's params and body, so it is argument-dependent by construction, yet its
estimate defaults to `Granted` (`funktor/rest/src/jvmMain/kotlin/auth/AuthRule.kt:51`, `AuthRuleBuilder.kt:84`). An
ownership check written with it therefore reads as full access: `canFullyAccess` is true for every user, and
`canPartiallyAccess` never fires.

## Spec

- [ ] Default `estimateFn = { ApiAccessLevel.Partial }` in both `forCall`s. A user-only `forCall` ("is admin") now
      reads `Partial`; its author passes `estimateFn` to say `Granted`.
- [ ] Audit every `forCall` in the repo (funktor modules, saas, demo): pass an explicit `estimateFn` where the rule
      depends on the caller only. Let the compiler/grep list be a lower bound — check `forAll`/`forAny` sub-builders too.
- [ ] Rewrite the now-wrong sentences: `ApiAcl.kt` KDoc, `ultra/codegen/.../ts/runtime/acl.ts`, the docs page
      (`docs-site/src/pages/ultra/funktor/auth.astro`) and LLM mirror (`docs-site/src/data/llms/funktor.md`) — "the
      built-in rules never produce `Partial`" stops being true.
- [ ] Tests: `ApiRouteEstimateAccessSpec` pins the default; the access-matrix e2e shows an ownership route as
      `Partial`. Mutation: the default back to `Granted` must go red.
