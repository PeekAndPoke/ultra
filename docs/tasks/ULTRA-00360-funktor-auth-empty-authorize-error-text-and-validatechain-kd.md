# ULTRA-00360: Funktor auth: empty-authorize error text and validateChain KDoc; sdkgen-app dev port

> **Status (2026-11-25): DESIGNED.** Maintainer: "Fix both"; implementation next.

## Why this exists

Found by the ULTRA-00356 truth audit:
1. `funktor/rest/src/jvmMain/kotlin/auth/AuthRuleBuilder.kt:182-183`: the empty-`authorize {}` error advises "or declare
   public() explicitly for a public route". Inside any `ApiRoutes` group a route's `authorize { public() }` always throws
   at construction, because the floor is prepended and `validateChain` allows a constant only as the sole rule. The
   KDoc at :131-132 still says "a route that never declares authorize has an empty chain and is legally public", but
   every route gets the floor (ApiRoutes.kt:216).
2. `funktor-demo/sdkgen-app/vite.config.ts` sets port 36591 without `strictPort`; b2b-app's webpack dev server uses
   36591 too (funktor-demo/b2b-app/webpack.config.d/webpack.js:17). Whichever starts second moves or fails.

## Decisions (maintainer, 2026-11-25)

- The error points to `authFloor = { public() }` on a public group; the KDoc describes the floor.
- sdkgen-app moves to the next free port in the series (36593) with `strictPort: true`; the server's CORS list and
  the sdkgen-app README follow.

## The work

- Auth: a test pins the new message text (what a developer reads); KDoc fixed.
- Port: vite config, CORS hosts in funktor-demo server.kt, sdkgen-app README, and any other mention of 36591 for
  sdkgen-app (grep). Check 36593 is unused in the repo.
- funktor rest suite green.

## Acceptance

- The empty-authorize error gives advice that works; the KDoc is true; sdkgen-app and b2b-app can run together.

## History

- 2026-11-25: created on the maintainer's decision.
