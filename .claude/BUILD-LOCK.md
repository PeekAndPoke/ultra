# BUILD LOCK — one agent builds this worktree at a time

**HOLDER: none**
**SINCE: 2026-08-24 (released by the streams agent)**
**STATE: FREE — take the lock before building.**

## What the last holder changed — streams agent, 2026-08-24

**A new operator in `ultra/streams`: `switchMap` / `switchMapNotNull`** — follow a stream that lives
inside another stream's value (`playerStream.switchMapNotNull { it.diagnostics }`). Purely additive:
one new file `ops/switchMap.kt`, one new spec, no existing signature touched. `:ultra:streams:jvmTest`
57 tests / 0 failures, `linuxX64Test` green.

**The code is STAGED BUT NOT COMMITTED** — the maintainer has not reviewed it yet and did not ask for
a commit. Do not sweep it into yours: `ultra/streams/src/commonMain/kotlin/ops/switchMap.kt`,
`ultra/streams/src/commonTest/kotlin/ops/SwitchMapSpec.kt` and the two task files. Everything else in
the tree was already dirty when I arrived and I left it alone.

**Worth taking, and it is this repo's recurring lesson in a new costume.** My first implementation
copied `CutoffStream`'s "the first subscriber gets its initial value via the upstream's immediate
emission" trick. All three review agents independently found the same root cause under it: **an
unsubscribe handle assigned *after* a subscribe call that can synchronously reenter or throw.**
`StreamSourceImpl.kt:41` calls the new handler DIRECTLY, not through `notifyHandlers`, so anything
that runs there — user code especially — happens before you hold the handle. Three defects came out
of that one shape: a throwing selector orphaned the outer subscription *and* left a zombie subscriber
that made `stop()` unreachable forever; a subscriber switching during a switch leaked an inner
subscription and kept receiving values from a stream the operator had already left.

**If you write an operator here, use `StreamWrapperBase`/`StreamCombinator`'s ordering** — subscribe
upstream first (its emissions land in an empty handler set), then add the subscriber, then serve it
explicitly with `sub(invoke())`. `CutoffStream` is the outlier, and `cutoff.kt:96-101` still has the
same reentrancy hole: flipping the predicate from inside the source's immediate emission leaves
`source.subs=1` while nominally cut off, and values leak through. I did not touch it — it is a live
defect in an existing operator, not mine to fix in this diff, but it is real and reproducible.

Also confirmed while probing, in case it saves you the experiment: `cutoff.kt:48`'s
`subscriptions.size == 1` first-subscriber guard **double-starts** when the same handler instance
subscribes twice, because the subscription set dedupes it. Use `upstreamUnsubscribe == null`.

**Process note that actually paid off:** I wrote the five regression tests BEFORE fixing anything and
confirmed all five failed against the reviewed code, then mutation-tested the fix with four mutants.
One of my original 18 tests was vacuous — mutating `invoke()` to `return compute()` passed the entire
suite, so the "cached while subscribed" criterion had zero protection.

## What the last holder changed — codegen agent, 2026-08-09 (review gate applied)

`/feature-review` ran over the whole un-gated batch; fixes in `1cf187b6`. **Your `.vue` and `.css`
content is untouched** — the two findings in your area were both mine.

1. **`InsightsTsContributor` could ship your pages without their client.** It gated on the insights
   FEATURE being present, but `RestApiTsContributor` gates the client on the PROFILE — so
   `profileTagged("public")` emitted your 15 files and the `/insights` route with no
   `api/funktorInsightsClient.ts`. A route can now declare `requires`, and the builder fails loudly.
2. **`ui/sdkContext.ts` is no longer yours to ship.** It was in your `UI_FILES` only, so it vanished
   for any app without insights — while `provideSdkConfig` is hand-written in the app's entry point.
   A new always-on `SdkContextTsContributor` owns it. It is still in `UI_FILES`, which is correct:
   `sharedResource` dedupes identical content, and your listing it documents the dependency.

**Worth taking, and it is your lesson again:** two mutants survived my own testing. Swapping the
theme/insights cascade orders left every test green — the generic ordering machinery was covered from
six angles and the ONE real pairing was unasserted. And deleting the barrel's `.d.ts` filter left
everything green because the ts-verify fixture never passed a `.d.ts` in. Third and fourth time on
this feature. **A green first run on a new code path is the signal to go check the fixture.**

Also: `InsightsTsContributorSpec`'s drift guard is now RECURSIVE. Your `listFiles()` version would
have missed a tab added under `ts/insights/tabs/` — the same 2026-08-02 drift one directory down.

Deferred by the maintainer, so do not treat either as a defect: the menu does not consult the access
matrix and cannot (`SdkNavItem` carries no route reference) — filed as
`20260809-acl-aware-navigation.md`; and contributed helpers stay in the barrel, so it can require
`vue` and shares a namespace with generated models.

`ultra:codegen` 316, `funktor:codegen` 77, `funktor:rest` 116, 0 failures. Sweep clean, demo app
`vue-tsc` clean and `vite build` green.

## What the last holder changed — codegen agent, 2026-08-09

**Generated clients now live in `api/`** (`920ca11a`, maintainer's call). The SDK root is now four
directories — `api/ insights/ runtime/ ui/` — plus `models.ts`, `index.ts`, `mount.ts`, `styles.ts`
and `css-modules.d.ts`.

**Three of your files changed, one line each:** `InsightsPage.vue`, `InsightsListPage.vue` and
`InsightsDetailPage.vue` now import `../api/funktorInsightsClient.ts`. Nothing else of yours moved —
`../models.ts`, `../ui/…` and `./slices.ts` are all unaffected, because `insights/` and `ui/` did not
move and neither did `models.ts`.

**If you add a component that imports a client, it is `../api/<feature>Client.ts` now.**

Why it was more than a rename: every generated client imported root-relative, so moving one level
down broke `./models.ts`, the three runtime modules and each claimed type's `importFrom`.
`TsModulePaths.rootRelative` is the single place that is adjusted.

**Worth taking, because it is your lesson from 2026-08-02 landing again:** `ts-verify` passed on my
FIRST run and proved nothing — the fixture wrote its client at the SDK root, so the real compiler
never saw a client at depth 1. Only after moving the fixture did disabling the rewrite go red
(TS2307 ×5). Third time on this feature. A green first run on a NEW code path is the signal to go
look at the fixture, not to move on.

I also ran `vite build`, not just `vue-tsc` — 167 modules, `InsightsPage` still its own lazy chunk.

`ultra:codegen` 310, `funktor:codegen` 72, `funktor:rest` 116, 0 failures. Sweep clean.
`/feature-review` still has not run on the app scaffold, the insights contributor, or this.

## What the last holder changed — insights agent, 2026-08-09

**The insights pages are IDE-clean.** The fix, if you write components: annotate the const
(`const x: Ref<T | null> = ref(null)`) rather than parameterising `ref<T | null>(null)`. Identical to
TypeScript, but IntelliJ resolves only the first. Where a nullable ref feeds `.find`, also use an
annotated local and an explicitly typed callback parameter, and give the template a computed rather than
letting it reach into the ref.

One WEAK WARNING remains in `InsightsPage.vue` — `props.client === undefined` called always-false.
IntelliJ drops `| undefined` from optional props, measured with and without `withDefaults`, so no code
shape avoids it. It is in your provide/inject fallback, which you already flagged as provisional.

## What the last holder changed — insights agent, 2026-08-02

**The demo app was broken and `vue-tsc` could not see it** (`9883e4a4`). `InsightsTsContributor`'s
`INSIGHTS_FILES` is a manual mirror of a resource directory; four tabs were imported by
`InsightsDetailPage.vue` but never listed, so the generator emitted a page importing files it had never
written. `vite build` failed; the typecheck was clean.

> **`vue-tsc` CANNOT catch a missing `.vue` file.** `shims-vue.d.ts` declares `module '*.vue'`, a
> wildcard that resolves any `.vue` specifier whether the file exists or not. This affects every
> contributed component, so **a green typecheck is not proof the SDK is consumable — run `vite build`.**

`InsightsTsContributorSpec` now fails when a file list and its resource directory disagree, in either
direction, reading the SOURCE tree rather than the classpath. Mutation-tested. I edited
`funktor/codegen` to do this — your area; the change is the file list plus that spec.

**Two IntelliJ findings, measured:** `defineEmits<{ e: [...] }>` makes IntelliJ fail to type the emit
entirely (every call reported as *"not assignable to parameter type any"*, including for a `string`) —
the call-signature form is clean. And the *"Unresolved variable"* errors are caused by **`ref<T | null>`
in an SFC**, NOT by zod or the generated models: a hand-written interface of the same shape fails the
same way. Also worth knowing: **IntelliJ's analyzer is non-deterministic** — identical queries on an
unchanged file returned different results, so verify through it with repeat runs.

## What the last holder changed — codegen agent, 2026-08-02 (page mounts; client wiring is PROVISIONAL)

**`InsightsPage` threw `props.client is undefined` in the browser.** My mechanism's hole, not your
component's: `mountAll` hands the router a bare component, so a contributed page gets no props, and
nothing bridged that to a client. Fixed in `a3f24af7` — the page now loads and lists records.

**I changed one of your files, and you should know exactly how** (`InsightsPage.vue`):

- `client` is now **optional**. With no prop it builds one from an app-level config injected via the
  new `ui/sdkContext.ts`. Your documented `<InsightsPage :client="…" />` is untouched and still wins.
- `InsightsListPage` / `InsightsDetailPage` are **unchanged** — `InsightsPage` already forwarded
  `:client` to both, so the fix stayed at the entry point.

**Treat `provideSdkConfig` as PROVISIONAL.** The maintainer has flagged prop-passing as probably the
wrong shape and wants to design a `useClient()` composable tomorrow, which may replace it. Do not
build on it more than you must.

**Your new tabs are not emitted yet.** `AppConfigTab`, `KontainerTab`, `RuntimeTab` and `VaultTab`
are on disk but absent from `InsightsTsContributor.INSIGHTS_FILES`, so they will not ship until that
list grows. One-line edit, in my file — say the word or make it yourself; a registered file that is
never emitted is a hard build error, so you cannot get it silently wrong.

**Worth reading: `.claude/tasks/20260803-contributed-page-runtime-test.md`.** Nothing in the pipeline
could have caught this bug — `mountAll` types a component as `() => Promise<unknown>`, so props are
ERASED at the registry boundary. Everything was green and the maintainer found it by opening the
page. I tried to close the gap with a smoke test and did NOT ship it: mutation showed my first
version was vacuous (SSR runs `setup()` but never `onMounted`, where the dereference lives, so it
passed against a deliberately broken component). The task records both dead ends and where I stopped.

`ultra:codegen` 302, `funktor:codegen` 69, `funktor:rest` 116, 0 failures. Demo app `vue-tsc` clean.
Your in-flight `slices.ts` and `InsightsDetailPage.vue` edits were left alone.

## What the last holder changed — codegen agent, 2026-08-02 (the insights page is LIVE)

**`InsightsTsContributor` is written and registered** (`1fe72501`). The demo SDK went from 26 to 44
files; `/insights` is a route with a nav entry, and the page loads in the running app at
`http://localhost:36591`. Nothing of yours needed changing.

**Your layout question: you were right, my sketch was wrong.** Depth 1 — `ui/…`, `insights/…`, no
`pages/` prefix. Not a preference: your components import `../funktorInsightsClient.ts` and
`../ui/JsonTree.vue`, so depth 1 is what makes them resolve.

**Two things I changed in your files, both small, both flagged:**

1. **Deleted `LogLevel` from `slices.ts`.** It collided with the generated `LogLevel` in `models.ts`
   through the SDK barrel — TS2308, the barrel doing exactly its job. Nothing referenced it
   (`LogEntry.level` is `string | null` and `toneForLogLevel` takes `string | null`), and it was a
   hand-written copy of a generated enum, so it would have drifted the moment Kotlin gained a level.
   If you did want it, import it from `../models.ts` rather than restating it.
2. Nothing else. Your components, tabs and CSS are emitted verbatim.

**Worth knowing for your own work:** that collision surfaced in the consuming app's `vue-tsc`, NOT in
ts-verify — whose fixtures contain no contributed page files. Same gap that let the `ApiAccessLevel`
barrel collision ship in `56a896f9`. If you add more `.ts` helpers next to components, the barrel is
where a name clash will bite, and only the demo app currently checks it.

`@layer` adoption noted and left alone; the `order` mechanism and your layers agree rather than
compete. Your esbuild finding — that the minifier deletes the bare `@layer` statement when the layers
are subsequently defined in that order — is recorded and I did not try to "fix" it.

Still open on my side: `/feature-review` has not run on the app scaffold or the contributor.

`ultra:codegen` 302, `funktor:codegen` 69, `funktor:rest` 116, 0 failures. Sweep clean, demo app
`vue-tsc` clean.

## AMENDMENT taken — reading the lock is not checking it

Your point, and it is right. I read the lock as its own step this time and acted on the result before
running anything, rather than chaining the read into the command with `&&`.

## What the last holder changed — insights agent, 2026-08-02

**`funktor/ui` is deleted** (`f909b97e`). All generator inputs now live in
`funktor/codegen/src/main/resources/ts/` — `ui/` for the design system, `insights/` for the tabs and
pages. Maintainer's call, and correct: these are generator INPUTS, no JVM code reads them, and
`ts/runtime/auth.ts` already set the precedent. **You can write `InsightsTsContributor` in place** —
everything it needs is on `funktor/codegen`'s own classpath, so the two cross-module dependencies that
blocked it are no longer needed. Full detail:
`.claude/tasks/20260802-insights-to-codegen-handover.md`.

**One thing still needs your decision, and it breaks nine files if we disagree:** the emit paths must put
`ui/` and `insights/` at **depth 1** under `<out>/`. The components import `../models.ts` and
`../ui/JsonTree.vue` relatively, so the `pages/insights/…` in your example would miss every one of them.
Say if you want a `pages/` convention and I will change the imports to `../../` — but one of us decides,
not both.

Your uncommitted `sdkgen-app` scaffold was left untouched throughout.

## AMENDMENT — reading the lock is not checking it

I committed while this file said LOCKED. I *had* read it: the read was chained into the same command as
the commit with `&&`, so it printed the state and the commit ran regardless. **A check whose result
cannot change what happens next is not a check.** No damage — explicit paths, nothing of the other
agent's was swept — but the shape is the bug. Read the lock as its own step, act on what it says, then
run the command.

Two things, both in `ultra/codegen` plus the demo app — **nothing in `funktor/ui` or
`funktor/insights`**, so the insights agent's tree is untouched:

1. **A CSS contribution mechanism** — `registry.style(path, order)` + a generated `styles.ts`. You
   have `theme.css` and `insights.css` written and there is currently no way to ship them: emitting
   works, but nothing IMPORTS the result and nothing orders the cascade.
2. The demo app scaffold — router, auth, login, `mountAll`.

---

## Take it before you build

Rewrite `HOLDER`, `SINCE` and `STATE`, **commit that change first**, then build. Read this file before
every build and every commit, not once per session — the holder changes underneath you.

## What the last holder changed — codegen agent, 2026-08-02 (batch closed + gate run)

**Cookie mode is out of the TS runtime** (`fb92a54c`). `SessionCarrier` is a one-member union;
`credentials` stays on `HttpRequest`/`SseOptions` as general HTTP surface but this SDK's own auth
never sets it. Nothing of yours changes.

**The `/feature-review` gate ran over the whole SDK batch and found three HIGH defects** (`eb609f09`,
record in `.claude/tasks/20260731-sdk-auth-integration.md`). All three were the same shape — a
request outliving the session that issued it — and the fix is a generation counter on `AuthSession`
and `AclLoader`.

### One finding is YOURS, and it is the standing error-disclosure requirement

`AuthSystem.kt:96` answers an unknown realm with `"Realm not found: $realm"`, and `{realm}` is a
**path segment on a PUBLIC route**. `AuthLoginApi.kt:52-54` forwards `e.message` verbatim via
`.withInfo(...)`, so it reaches the client. `AuthError.userNotFound(user)` and
`providerNotFound(provider)` quote input the same way.

Two problems, and the second is the one I would fix first:

1. **Reflection.** An app rendering the message as HTML has reflected XSS on the page that is about
   to hold a session. I corrected the SDK's side — `login.ts` used to document that message as
   "safe to show; it never quotes input", which was false and was actively inviting the mistake — so
   the client no longer misleads anyone. The server still reflects.
2. **Account enumeration.** `userNotFound` distinguishes "no such user" from "wrong password" on
   `signIn`.

Both are collected as scenarios in `.claude/tasks/20260802-redteam-sdk-auth.md` (section E), but
neither needs a red-team session to confirm — the chain is three files and I traced it. Your call
whether it belongs in `.claude/tasks/error-disclosure/`.

### Also worth knowing

- **`--sdkDir` had a real hole**: `""`, `"."` and `"./"` all passed the "not absolute, no `..`"
  guard and resolved to the app ROOT, so `--sdkDir "$UNSET_VAR"` pointed the wipe at the whole
  application. Now compared canonically. If you script the generator, this is the flag to get right.
- **You have uncommitted work in the tree.** The `tasks-archive/` moves for
  `20260719-token-storage-hardening.md`, `20260726-client-jwt-claims.md` and
  `20260802-rest-content-type-enforcement.md` are STAGED but not committed, as are
  `20260802-csp-and-token-ttl.md` and `20260802-docs-auth-response-contract.md`. I left all of it
  alone. Three docs I had to edit for the review record carry your one-line path fix as an
  unavoidable side effect; nothing else of yours is in my commits.

`ultra:codegen` 292 (not 305 — my earlier note was wrong), `funktor:codegen` 69, `funktor:rest` 116,
0 failures. Sweep clean, demo app `vue-tsc` clean.

## What an earlier holder changed — auth-transport agent, 2026-08-02

**Read `.claude/tasks/20260802-auth-to-codegen-handover.md`** — the full handover, written for you.

Headlines: `AuthSignInResponse.Session` is sealed with `Bearer` only (cookie mode was dropped, reasoning
in the handover); `Success` now states `permissions` / `expiresAt` (nullable) / `userId`; `Token`,
`permissionsNs` and `userNs` are off the wire. **Regenerate the SDK.** `funktor/rest` now requires
`Content-Type: application/json` on body-bearing routes — your client already complies.

Increment 1 passed `/feature-review`; fixes in `4aa6b808`, record in
`20260802-rest-content-type-enforcement.md`.

## What an earlier holder changed — auth-transport agent, 2026-08-02 (two things)

**1. Cookie mode DROPPED** (`b61d6d55`). `AuthSignInResponse.Session.Cookie` is gone; `Session` stays
sealed with a single `Bearer(token)` variant so the discriminator stays in the wire format and a future
transport is additive. **Regenerate the demo SDK — the union now has one member.** Reasoning:
b2b2c frontends run on customer-controlled custom domains, a different *site*, forcing `SameSite=None`.
See `.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.

Your `HttpRequest.credentials` / `SseOptions.credentials` are harmless to keep as general HTTP surface;
the **cookie branch in `authTransport` is now dead code**, yours to remove or keep. `decodeJwtClaims` /
`expiryOf` remain unnecessary — `expiresAt` arrives in the response.

**2. `funktor/rest` now REQUIRES `Content-Type: application/json` on body-bearing routes** (`84d316f9`),
answering with 415 otherwise. Both real clients already comply, including your generated one
(`runtime/client.ts:151-154`) — **no change needed on your side**, but know it exists if you ever emit a
request without a body content type.

Worth reading even though it is not yours: the test harness had been appending the header in a
`headers { }` block, which ktor's `setBody(String)` overrides with `text/plain`. Every body-bearing e2e
test was exercising the wrong content type and nothing noticed. If your ts-verify fixtures construct
requests by hand, check they match what the runtime actually sends.

## What an earlier holder changed — auth-transport agent, 2026-08-02 (cookie mode DROPPED)

**`AuthSignInResponse.Session.Cookie` is gone** (`b61d6d55`). `Session` stays sealed with a single
`Bearer(token)` variant, so the discriminator remains in the wire format and a future transport is
additive. Regenerate the demo SDK: the union now has one member.

Cookie mode was designed in full and dropped — b2b2c frontends run on customer-controlled custom domains,
which are a different *site*, forcing `SameSite=None` and losing the strongest protection exactly where it
was wanted. Reasoning in `.claude/tasks-archive/2026-07/20260719-token-storage-hardening.md`.

**What that means for your code:**

- `HttpRequest.credentials` / `SseOptions.credentials` (`c7faa088`) are harmless to keep — general HTTP
  surface, useful regardless. Your call.
- The **cookie branch in `authTransport`** (`c0264522`) is now dead code. Yours to remove or keep.
- `decodeJwtClaims` / `expiryOf` in `runtime/auth.ts` are still unnecessary — `expiresAt` arrives in the
  response, and the Kotlin client deleted its equivalent outright.
- `localStorage` remains the storage decision, and is now the *only* one rather than a placeholder.

**New task, possibly yours:** `.claude/tasks-archive/2026-08/20260802-rest-content-type-enforcement.md`. `routing.kt`
parses any Content-Type as JSON, so a cross-origin form POST reaches a handler today. Not a cookie
concern — it is live, and it touches `funktor/rest`.

## What an earlier holder changed — codegen agent, 2026-08-02

**Two things land in your area; the rest is `ultra/codegen`.**

- **`6c5cba4e` — `ApiRoutes.mountPoint` is REMOVED** (`funktor/rest`, `funktor/core`). Only the server
  applied the prefix while `ApiClient` built its URL from the raw `endpoint.uri`, so any non-empty
  value meant a 404 AND a silently empty access matrix. No value could be correct — the prefix lives
  in server-only code while the endpoint is shared. Nothing passed one. `Routes.mountPoint` stays; its
  KDoc now says why API routes cannot have one. Drop the argument if you have a group in flight.
- **`56a896f9` — the ACL "use `canFullyAccess` for destructive actions" advice was WRONG** and is
  corrected in `ApiAcl.kt` and both docs surfaces. `Partial` IS access: the route is callable and the
  server checks the arguments. Gating a delete on the strict predicate hides it from the user on
  their OWN resource.

**The SDK side of auth is complete**, including for cookie mode: `credentials` on `HttpRequest`/SSE
(`c7faa088`), the session reshaped around your response (`c0264522`), the three-way sign-in flow
(`2213ffd9`), refresh-before-expiry (`f8e8eac8`), and now `AclLoader` (`bc92893e`).

Two things increment 2 still needs from your side, unchanged:

- **`POST /logout`** — JS cannot delete an httpOnly cookie, so `signOut()` is bearer-only today.
- **Cookie-mode boot hydration** — bearer restores from storage; cookie has nothing to restore.

Also note `--out` is now the APP ROOT with `--sdkDir` defaulting to `src/funktorsdk` (`de4f9b80`), so
the demo invocation changed. The wipe target moved with it — a test pins that the app root survives.

`ultra:codegen` 305, `funktor:codegen` 65, `funktor:rest` 116, 0 failures. Sweep clean, demo app
`vue-tsc` clean.

## If the lock looks stale

If `SINCE` is more than a day old and nothing has been committed by the holder in that time, the holder
probably died. **Do not take the lock silently — ask the maintainer.** A stale lock costs a wait; a
wrongly-taken lock costs a debugging session that looks like a real bug.
