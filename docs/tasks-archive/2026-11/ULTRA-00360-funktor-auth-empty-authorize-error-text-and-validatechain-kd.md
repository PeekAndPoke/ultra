# ULTRA-00360: Funktor auth: empty-authorize error text and validateChain KDoc; sdkgen-app dev port

> **Status (2026-11-25): DONE.** Gate PASS in round 1; merged into master.

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

## Implementation notes

- `funktor/rest/src/jvmMain/kotlin/auth/AuthRuleBuilder.kt:190`: the empty-`authorize {}` error now says: add a rule,
  or remove the block so the route keeps just its group floor; for a public route, use a separate group with
  `authFloor = { public() }` and no `authorize {}`, because a route's own `public()` conflicts with the floor; and
  changing an existing group's floor to `public()` opens every route in it that has no `authorize {}`.
- `AuthRuleBuilder.kt:171-183`: the KDocs of the route-level `public()` and `forbidden()` say they always conflict
  with the group floor inside an `ApiRoutes` group, and point to `authFloor = { public() }` / `{ forbidden() }`.
- `AuthRuleBuilder.kt:131`: the `validateChain` KDoc no longer claims an empty chain is legally public. It says the
  callers check non-emptiness (all four production callers do: `RootAuthRuleBuilder.build`,
  `FloorAuthRuleBuilder.build`, `ApiRoutes.addRoute`, `AuthChainBootCheck`) and that every `ApiRoutes` route carries
  its group floor.
- `funktor-demo/sdkgen-app/vite.config.ts:8-10`: port 36593, `strictPort: true`.
- `funktor-demo/server/src/main/kotlin/server.kt:58-60`: sdkgen-app CORS hosts moved to 36593. The b2b-app hosts on
  36591 stay.
- `funktor-demo/sdkgen-app/README.md:25,29,34-36`: port 36593. The note that Vite drifts to the next free port is
  replaced: the port is strict now.
- Grep: before the change, 36593 appeared nowhere in the repo. The remaining 36591 hits outside `docs/` are all
  b2b-app (webpack config, application.dev/test.conf, server.kt b2b CORS). `.claude/BUILD-LOCK.md:448` names
  `http://localhost:36591` in an old log entry; left alone (master-only file, historical record).

## Test evidence

- New tests: `AuthRuleBuilderSpec.kt:194` pins the whole error message with `shouldBe`;
  `FloorAuthRuleBuilderSpec.kt:111` shows a route's own `authorize { public() }` in a public-floored group fails at
  construction ("SOLE rule"). The second pins existing behaviour (the premise of the new advice), so it was green
  before the fix by design.
- RED before the fix: `:funktor:rest:jvmTest`, XML 126 tests, 1 failure (AuthRuleBuilderSpec, expected the new
  text, got "... or declare public() explicitly for a public route.").
- Mutants (AuthRuleBuilder.kt, cp backup, restored, cmp clean): M1 `authFloor = { public() }` changed to `public()`
  in the message: killed (1 failure). M2 old message restored: killed (1 failure).
- Green: `:funktor:rest:jvmTest`, XML 16 suites, 126 tests, 0 failures, 0 errors, 0 skipped.
  `:funktor-demo:server:compileKotlin` green.
- Vite: `pnpm install --frozen-lockfile`, then `vite.resolveConfig({}, 'serve')` loads `vite.config.ts`:
  port 36593, strictPort true (vite 8.2.0). No dev server started.

## Review ledger

Round 1, standard gate, one reviewer covering the three charters (implementation and code style, domain, security),
rung 1 reviewer-opus-high. Verdict: PASS, 0 CRITICAL, 0 MAJOR.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MINOR | AuthRuleBuilder.kt:190 | "mount it in an ApiRoutes group with authFloor = { public() }" reads as "change this group's floor", which opens every sibling route without `authorize {}` | fix | now "a separate ApiRoutes group", plus a sentence that changing an existing group's floor opens those routes; pinned test string updated; mutant without "separate" RED |
| 1 | MINOR | AuthRuleBuilder.kt:171-183 | the route-level `public()`/`forbidden()` KDocs do not say they always conflict with the group floor | fix | one line each, pointing to `authFloor = { public() }` / `{ forbidden() }` |

CLEAN: every statement in the error message and the KDocs is true when followed literally; all four `validateChain`
callers check non-emptiness; no other place gives the old advice; no sdkgen-app 36591 left; the CORS hosts are
correct; vite `strictPort` is set.

Round 1 fixes: `:funktor:rest:jvmTest` green, XML 16 suites, 126 tests, 0 failures, 0 errors, 0 skipped.

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: IN PROGRESS. Implemented on wt/auth-port; review gate next.
- 2026-11-25: gate round 1 PASS (0 CRITICAL, 0 MAJOR); two MINORs fixed. Ready to merge.
- 2026-11-25: DONE.
