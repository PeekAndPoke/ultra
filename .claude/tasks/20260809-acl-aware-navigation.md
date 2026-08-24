# ACL-aware navigation for contributed pages

**Status:** DONE — 2026-08-24. `/feature-review` PENDING; see the gate note at the bottom.
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

## Gate

`/feature-review` has NOT run on this change yet. Run it before archiving this file.
