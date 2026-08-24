# ACL-aware navigation for contributed pages

**Status:** DONE — 2026-08-24, gate PASSED. Ready to archive.
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`
**Security-critical:** no. The client-side ACL is ADVISORY — the server is the authority — so this is
about telling the user the truth, not about enforcement. It is still access-shaped, so it was
mutation-tested; see below.

## The gap it closed

The demo app loaded the access matrix, subscribed to it, and **never read its contents**. The
matrix's entire observable effect was to blank the menu while it was in flight, and `App.vue`
claimed to be "gated on the ACL" when it never was — caught by `/feature-review` on 2026-08-09.

What a user saw: an ordinary operator signed in, saw "Insights", clicked it, and landed on a page
whose every call 403s. `InsightsApi` floors at `isSuperUser()`
(`funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:21`), and `requiresAuth` cannot stand in for
that — it is declared, deliberately coarse, and true for any session at all.

## What was built

**`TsSdkRegistry.Nav.requires: List<ApiRouteRef>`** — the API routes a page cannot function without,
resolved at GENERATION time to the `method` + route-PATTERN pair the access matrix is keyed on, plus
the route's publicness. Emitted into `SdkNavItem.requires`; the app filters
`navItems.filter(i => i.requires.every(r => acl.canAccess(r)))`.

Four decisions, all taken by the maintainer on 2026-08-24:

| Question | Answer |
|---|---|
| Which predicate? | `canAccess` — it includes `Partial`, and `Partial` IS access. Settled by `ApiAcl`'s own KDoc, so it was never in doubt. |
| ALL or ANY? | **ALL**, with the convention "declare the MINIMUM the page cannot function without". With one entry the two coincide, which is the common case; ALL is the clearer rule when there are several. |
| Guard too, or menu only? | **Menu only.** A redirect driven by a client-side ADVISORY matrix is the worse failure: a matrix that failed to load, or a stale baked `isPublic`, would lock a legitimate user out of a page the server would happily serve. Typing the URL still reaches the page and it 403s honestly. |
| Where do method+uri come from? | **Resolved against the LIVE route graph**, via `TsRouteRefs.of(feature, member)`. |

### The fourth answer corrected the premise of this task

This file used to say the pair was "reachable at runtime but not at Kotlin emit time". That was
**wrong**. `InsightsTsContributor` already takes `Lazy<List<ApiFeature>>`, and
`ApiFeature.getRouteGroups()` → `ApiRoute.method.value` / `pattern.pattern` is exactly the pair
`RestApiTsContributor.kt:244` emits into the generated client. `isPublicToAnonymous` is evaluable
there too. So no contributor hand-copies a URI, and the whole class of silent drift the sketched
literal would have introduced does not exist.

That matters more than it sounds: `ApiAcl` transmits denial by OMISSION — the server withholds
`Denied` rows so the matrix does not disclose the API surface — so a ref matching no row reads
`Denied` for **every** user. A stale literal would hide the menu entry from everybody, permanently,
with nothing anywhere naming the cause. Now a rename fails the build and lists the real member names.

## Files

| File | What |
|---|---|
| `ultra/codegen/src/main/kotlin/sdk/TsSdkRegistry.kt` | `ApiRouteRef`, `Nav.requires`, and the two validations |
| `ultra/codegen/src/main/kotlin/ts/TsMountEmitter.kt` | `SdkApiRouteRef`, the `requires` field, the derived `HttpMethod` union |
| `funktor/codegen/src/main/kotlin/TsRouteRefs.kt` | NEW — the live-route-graph lookup, plus `isPublicToAnonymous` moved here |
| `funktor/codegen/src/main/kotlin/InsightsTsContributor.kt` | gates `/insights` on `listRecords` |
| `funktor-demo/sdkgen-app/src/App.vue` | `readableAcl()` + the two-gate `visibleNav()` |

Two design points worth not re-deriving:

- **`SdkApiRouteRef` is DECLARED in `mount.ts`, not imported from `runtime/route.ts`.** Runtime
  modules ship only when something imports them (`TsRuntime`); `mount.ts` is unconditional. An import
  would break every SDK that reaches no API client at all.
- **The name differs from `RouteRef` deliberately.** The barrel `export *`s both files. Proved, not
  assumed: renaming it to `RouteRef` makes real `tsc` fail with `TS2308` — see the mutation table.

## Test evidence

`ultra:codegen` **325**, `funktor:codegen` **88**, `funktor:rest` **116** — 0 failures, counts read
from `build/test-results/**/TEST-*.xml`. Compile sweep clean. Demo app `vue-tsc` clean and
`vite build` green against a freshly generated SDK.

- [x] `TsRouteRefsSpec` (NEW, 9) — method/pattern resolution, the DERIVED member name, publicness from
      three different floors, unknown-member error listing what exists, cross-group ambiguity and its
      `group =` escape, and that a resolved ref passes the registry's own validation
- [x] `TsSdkRegistrySpec` — requirements round-trip, empty is `emptyList` not null, an unlisted method
      and a non-pattern uri are both refused
- [x] `TsSdkMountBuildSpec` — the literals are emitted, an ungated entry emits `requires: []`, the
      shape is declared rather than imported, and the emitted union matches the validated set
- [x] `InsightsTsContributorSpec` — `/insights` gates on `GET /_/funktor/insights/records`,
      `isPublic: false`; and renaming the endpoint fails the build
- [x] `ts-verify` — real `tsc` + execution: `acl.canAccess(navItem.requires[k])` COMPILES (the only
      proof the two independently declared shapes still agree), one denied requirement hides the
      entry, ANY would not have, `Partial` counts as access, and an anonymous visitor keeps a
      public-gated entry
- [x] `FxInsightsApiRoutes` changed from `public()` to `forRole("super-user")` — it mirrors the real
      `isSuperUser()` floor, and under `public()` every gating assertion read `isPublic: true` and
      would have proved nothing

### Mutation table — all five killed

| Mutant | Killed by |
|---|---|
| `isPublic` hardcoded `false` | `TsRouteRefsSpec` "a public() floor yields true" |
| `SdkApiRouteRef` renamed to `RouteRef` | `tsVerify` — real `tsc`, `TS2308` on the barrel |
| every nav entry emits `requires: []` | `TsSdkMountBuildSpec`, AND independently 3 `tsVerify` checks |
| the validation loop removed | `TsSdkRegistrySpec`, 5 cases |
| the live lookup replaced by a literal `ApiRouteRef` | `InsightsTsContributorSpec` "renaming the gated endpoint fails the build" |

## Known limitation — stated, not hidden

**Nothing tests `App.vue`'s filter.** `ts-verify` proves the emitted DATA is right and that the two
TypeScript shapes agree, but it applies its own `every` — so swapping `App.vue`'s `every` for `some`
would not be caught by anything. That is precisely the gap
`.claude/tasks/20260803-contributed-page-runtime-test.md` describes, and it stays open.

## Also fixed in passing

`funktor-demo/sdkgen-app/README.md` told you to run the generator with
`--out …/src/funktorsdk`. Since the `--out`/`--sdkDir` split (2026-08-02) `--out` is the APP ROOT, so
that command writes into `src/funktorsdk/src/funktorsdk`, reports success, and leaves the real SDK
stale — the app then type-checks against yesterday's output. Hit while doing this work.

## Review record — /feature-review, 2026-08-24

Base `5ab46234` → `62b3f0a2`. Fixes in the follow-up commit.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | FAIL -> fixed | 7 LOW |
| 2. Domain expert | FAIL -> fixed | 1 MEDIUM, 5 LOW |
| 3. Security | FAIL -> fixed | 3 LOW |

**Verdict: PASS.** No CRITICAL or HIGH. Every finding was verified against the code before acting;
none was dropped as unconfirmed, which is itself unusual and worth noting.

### The MEDIUM, found independently by two reviewers — and it was mine from the last batch

**`loading.stale` is structurally unreachable in this app, so the menu blanks on every token
refresh** — while the KDoc I wrote said the opposite. `AclLoader.load()` computes `stale` from its
own `ready` state; the only path into it is `ensureAcl()`, which fires only when the state is
`absent`; and the refresh handler calls `clear()` first, deliberately.

Honouring `loading.stale` was itself a `/feature-review` fix on 2026-08-09 — and the `absent` guard
and the `clear()`-before-reload added **in the same batch** made it dead on arrival. The fix never
did anything, and this change restated its justification in new prose. Two comments then contradicted
each other across two files.

Kept the behaviour, corrected the comment: the blank is the FAIL-CLOSED direction, and the
alternative renders pre-refresh permissions during the reload window, so a revocation would not show
until the new matrix lands.

### Fixed

- **Three assertions satisfiable by a substring** — the repo's signature failure mode, and all three
  were in tests written for this change. `"listRecords"` is a substring of `"listRecordsV2"`, so it
  was satisfied by the known-endpoints list alone; `"/x"` is a substring of the ref uri `"/api/x"`,
  so the route path could be dropped from the message; and per-method containment on the emitted
  `HttpMethod` union cannot see an EXTRA member. Each tightened, then each **proved to fail** under
  the mutant it was supposed to catch.
- **`visibleNav` blanked the WHOLE menu while the matrix loaded**, including entries that declare no
  requirements — breaking the "empty means never hidden on access grounds" promise this change's own
  KDoc makes. A second module's public page would vanish for the length of an unrelated fetch.
- **`Route.requires` renamed to `requiresFiles`.** Two unrelated `requires` sat four lines apart in
  one `route(...)` call meaning emitted FILES and API ROUTES; both reviewers read them as one concept.
  Renamed on the Route side because it has no emitted counterpart, so `Nav.requires` and
  `SdkNavItem.requires` stay in step.
- **`isPublic` is the one field that fails OPEN and is unvalidatable here** — `ultra:codegen` has no
  route graph. Documented on the field and on the validator, which claimed to reject anything that
  "could never match a matrix row" while saying nothing about the field that bypasses matching.
- **The ambiguity message prescribed a fix that cannot work** when two `ApiRoutes` INSTANCES share a
  name — `group =` filters on the name, so it reproduces the same error. `funktor:auth` declares
  `ApiRoutes("login")` twice, so the shape is real. Now detected and answered differently.
- **`.gitignore` carried a second copy of the stale generate command** — the twin of the one the
  README fix corrected. Replaced with a pointer, so there is one copy.
- `TsRouteRefs`' KDoc claimed the member name proves the client has it. It does not; see below.

### Recorded, NOT fixed — with the reason

- **The nav gate is opt-in and fails open.** `requiresAuth = true` + `requires: []` gets no signal.
  Legitimate pages call no gated endpoint, the registry has no advisory channel, and the emitted
  `mount.ts` shows `requires: []` per entry, which is the visible affordance. Red-team item 15.
- **Stale `isPublic`, and no CI runs `--check`.** Confirmed: there is no `.github/` and `--check` is
  wired into no Gradle task. Pre-existing — every generated member has baked the same value from the
  same function since `route()`/`publicRoute()` shipped. Red-team item 14.
- **A profile can strip the gated member while the client still emits**, so a page calls a member
  that is not there. Neither side can see the other, so closing it needs a new registration — filed
  as `.claude/tasks/20260824-nav-requires-endpoint-crosscheck.md`.

### Clean, recorded so it is not re-trodden

- **`mount.ts` shipping method + uri + publicness is NO disclosure delta.** The generated client in
  the same bundle already carries the identical triple for every route, publicness included
  (`route()` sets false, `publicRoute()` true). `ApiAccessDescriptor` omits `Denied` rows to hide
  *which routes THIS user can reach* — a per-user fact — never *which routes exist*.
- **No injection path.** `tsStringLiteral` escapes `\`, `'`, LF, CR, U+2028 and U+2029 — the complete
  set of ECMAScript literal terminators — and the uri reaches string-VALUE position only. Materially
  unlike the `funcName` precedent, which landed in identifier position with no escaping at all.
- **No cross-user matrix leak.** `sdk.ts` keys on the TOKEN, `clear()` bumps the generation and drops
  any in-flight fetch, and the window between `signOut()` and `clear()` is covered by
  `readableAcl()`'s `!isLoggedIn → ApiAcl.empty` short-circuit.
- **`isPublicToAnonymous` moved without behaviour change** — body byte-identical, no shadowing member
  on `ApiRoute`, and `estimateAccess(User.anonymous)` folds the real chain including the group floor.
- **Nothing became load-bearing for authorization.** `router.beforeEach` still reads only
  `meta.requiresAuth`. `InsightsApi`'s `isSuperUser()` floor is untouched; the only auth-rule edit in
  the diff is a test fixture, and it TIGHTENS.
- **Emitted output is stable**: `navRoutes()` sorts by `(order, path)`, `requires` order is
  contributor-declared, nothing iterates a hash structure.
- Style: no wildcard imports, no fully-qualified names, KDoc links resolve.

### Red-team

Four scenarios collected into `.claude/tasks/20260802-redteam-sdk-auth.md` section G (items 14-17).
COLLECTED, not executed.
