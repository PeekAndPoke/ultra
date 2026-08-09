# ACL-aware navigation for contributed pages

**Status:** TODO — raised by `/feature-review` 2026-08-09, deferred by the maintainer in the same
session so the API gets designed rather than bolted onto a review fix.
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md`
**Security-critical:** no. The client-side ACL is ADVISORY — the server is the authority — so this is
about telling the user the truth, not about enforcement.

## The gap

The demo app loads the access matrix, subscribes to it, and **never reads its contents**. Verified by
grep: `ApiAcl.canAccess` / `canFullyAccess` / `isDenied` are called nowhere in
`funktor-demo/sdkgen-app/src/`. The matrix's entire observable effect was to blank the menu while it
was in flight.

`App.vue` claimed to be "gated on the ACL". It never was. That claim is now corrected rather than
left to mislead the next reader.

**What a user sees today:** an ordinary (non-superuser) operator signs in, sees "Insights" in the
menu, clicks it, and lands on a page whose every call 403s. `InsightsApi` floors at `isSuperUser()`
(`funktor/insights/src/jvmMain/kotlin/api/InsightsApi.kt:21`).

## Why it cannot just be fixed

`SdkNavItem` carries `path`, `label`, `icon` and `requiresAuth` (`TsMountEmitter`), and
`TsSdkRegistry.Nav` carries the same. **Nothing identifies the ROUTES a page calls**, so there is
nothing to look up in the matrix — `ApiAcl` is keyed on `method|uri`.

`requiresAuth` cannot stand in for it: it is DECLARED by the contributor and deliberately coarse,
because a logged-out visitor cannot fetch a matrix at all. That is why page gating and API-route
publicness are separate mechanisms, and that separation is correct.

## Sketch, to be designed properly

Let a contributor declare what its page needs, and filter on it:

```kotlin
context.registry.route(
    path = "/insights",
    component = "insights/InsightsPage.vue",
    requiresAuth = true,
    nav = TsSdkRegistry.Nav(label = "Insights", icon = "gauge", order = 10),
    // NEW: the routes the page cannot function without.
    navRequires = listOf(TsSdkRegistry.RouteRef("GET", "/_/funktor/insights/records")),
)
```

emitted into `SdkNavItem`, and applied as `navRequires.every(r => acl.canAccess(r))`.

Open questions worth settling before writing it:

- **Which predicate?** `canAccess` includes `Partial`, which is right for "should this appear" —
  see `ApiAcl`'s KDoc, corrected on 2026-08-02 after getting exactly this backwards.
- **ALL or ANY?** A page calling five endpoints where the user holds one: hide it, or show it and let
  the page degrade? Probably ANY for visibility and let the page handle the rest, but that is a
  judgement.
- **Does the route guard use it too**, or only the menu? A guard that redirects on a client-side
  advisory check is a worse failure mode than a page that 403s honestly.
- **Where does the method+uri come from?** Hand-written in the contributor duplicates what the
  generated client already knows. The generated member carries `method` and `uri` as `RouteRef` —
  reachable at runtime but not at Kotlin emit time. Possibly the app wires this rather than the
  contributor.

## Do not

Do not filter on `requiresAuth` and call it ACL gating. That is what was there, and the comment
claiming otherwise is what made it look done.
