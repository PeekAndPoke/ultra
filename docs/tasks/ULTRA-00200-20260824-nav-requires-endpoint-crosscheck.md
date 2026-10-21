# ULTRA-00200: Cross-check nav requirements against the endpoints a profile actually emitted

> **Status (2026-08-24): NOT STARTED.** raised by `/feature-review` 2026-08-24 (reviewers 1 and 2, independently).

**Plan:** ULTRA-00153 (frontend sdk vue contributors)
**Source feature:** ULTRA-00196 (acl aware navigation)
**Security-critical:** no. The gate itself stays CORRECT under a profile — this is a missing build
guard, not a wrong access decision.

## The gap

`TsRouteRefs.of` walks `feature.getRouteGroups()` **unfiltered**
(`funktor/codegen/src/main/kotlin/TsRouteRefs.kt:35`), while `RestApiTsContributor` emits from
`group.all.filter(include)` (`RestApiTsContributor.kt:187`). A profile narrows the second and not the
first.

**Scenario.** `funktorCodegen { profileTagged("ops") }` with `InsightsApi.getRecord` tagged and
`listRecords` not:

1. One route survives, so `api/funktorInsightsClient.ts` IS emitted.
2. `Route.requiresFiles = listOf(CLIENT_IMPORT)` therefore passes — it checks for a FILE.
3. `TsRouteRefs.of(feature, "listRecords")` passes too — the server route still exists.
4. `InsightsListPage.vue:55` calls `props.client.insights.listRecords(...)`, a member the emitted
   client no longer carries.

Caught only by `vite build` in someone's frontend — precisely the failure class the `requiresFiles`
mechanism was added on 2026-08-09 to move earlier. `vue-tsc` cannot see it either, for the usual
reason: the app's `shims-vue.d.ts` wildcard resolves any `.vue`.

**Note what is NOT wrong:** the access gate answers truthfully. The matrix is built from the server's
full graph with the same two fields (`ApiAccessDescriptor.kt:33-35`), so a profile-excluded route
still has a matrix row.

## Why it was not fixed in the source change

Neither side can see the other. The contributor never receives the profile —
`index_jvm.kt:62` re-registers only `RestApiTsContributor` with it — and `TsSdkBuilder`, the one
place with the full output plan, knows which FILES were emitted but nothing about the ENDPOINTS
inside them. Closing this needs a new registration: `RestApiTsContributor` publishing the
(file → emitted members) map into the registry, and the builder checking `Nav.requires` against it.

That is a mechanism, not a fix, so it was filed rather than bolted on.

## Sketch

- `TsSdkRegistry.endpoint(file, method, uri, member)` — declared by whoever emits a client.
- Builder check after every contributor has run: every `Nav.requires` ref must match a declared
  endpoint, failing with the page path, the ref and the profile as the likely cause.
- Bonus, and arguably the bigger win: the same registration would let a PAGE declare the members it
  calls, closing the `InsightsListPage.vue` case above directly rather than only the nav gate.

## Do not

Do not "fix" this by filtering `TsRouteRefs.of` through `include`. The nav gate must reference the
route the SERVER serves — that is what the matrix is keyed on — and a profile-excluded route is still
served. Filtering there would silently drop the gate instead of reporting the mismatch.

## History

- 2026-10-21: numbered ULTRA-00200 (was `docs/tasks/20260824-nav-requires-endpoint-crosscheck.md`).
