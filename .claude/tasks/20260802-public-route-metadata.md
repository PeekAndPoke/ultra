# Public-route metadata in the generated TypeScript SDK

**Status:** IN REVIEW — implemented 2026-08-02, `/feature-review` gate NOT yet run
**Plan:** `.claude/tasks/20260730-frontend-sdk-vue-contributors.md` → the auth/login family
**Security-critical:** yes — it widens what an anonymous client offers to try. See the scope note.

Follows `.claude/tasks/20260801-sdk-route-access-control.md` (the `route()` wrapper and `ApiAcl`).
Precedes the aggregation registry and `AuthTsContributor`.

## Why

`ApiAcl` currently denies everything to a logged-out visitor, **including `signIn`**. The matrix comes
from `AuthUserApi.getMyApiAccess`, which itself floors `authenticated()`
(`funktor/auth/src/jvmMain/kotlin/api/AuthUserApi.kt:20`), so an anonymous visitor has no matrix at
all and `ApiAcl.empty` answers `Denied` for every route. A frontend that gates its sign-in button on
the ACL locks the user out unrecoverably.

The generator can answer this without a matrix, because publicness is a property of the route rather
than of the user.

## The mechanism — an evaluation, not an enumeration

```kotlin
val isPublic = !route.estimateAccess(User.anonymous).isDenied()
```

Both halves verified before designing:

- `AuthRule.EstimateCtx` holds **only** a `User` (`funktor/rest/src/jvmMain/kotlin/auth/AuthRule.kt:144-146`),
  and `phase1Denials`' KDoc states `estimate` "needs no params" (`ApiRoute.kt:101-104`). So the whole
  chain is evaluable at GENERATION time — no server, no request, no session.
- `User.anonymous` is a ready-made singleton (`ultra/security/src/commonMain/kotlin/user/User.kt:13`).

**This is not the `getMyApiAccess` endpoint.** It is the same evaluator the matrix is built on, called
in-process. The generator already holds the `ApiRoute` objects — `RestApiTsContributor` walks
`feature.getRouteGroups()` → `group.all` — so it is a direct call, with no `ApiAccessDescriptor` and
no HTTP.

**Why not mirror the rules.** Emitting `forRole('ops-admin')` into TypeScript would mean a second
permission engine to keep in step forever, and the authorization model shipped in a public bundle.
It is also redundant: the per-user answer already exists, computed server-side, in the matrix.

`estimateAccess` folds the entire chain with `and`, so composition is handled by construction:

| chain | `estimate(anonymous)` | |
|---|---|---|
| `public()` | `PublicRule` → `Granted` | public |
| `authenticated()` | `isAuthenticated` false → `Denied` | protected |
| `forRole("ops-admin")` | `PermissionsCheck` vs `UserPermissions.anonymous` → `Denied` | protected |
| `isSuperUser() or forRole("x")` | fold of two `Denied` → `Denied` | protected |
| a rule kind nobody has written yet | whatever its own `estimate` says | correct by construction |

That last row is the point. The alternative design silently breaks every time someone adds a rule
kind; this one cannot.

### FINDING — that last row is currently UNREACHABLE, and the design still stands

Found while trying to build a fixture for it. **Today only `public()` can admit an anonymous caller**,
so `isPublic` is in practice equivalent to "the floor is a sole bare `public()`":

- `authFloor` is mandatory and validated non-empty (`ApiRoutes.kt:50,62`)
- every non-constant leaf (`authenticated`, `isSuperUser`, `forUserType`, `forGroup`, `forRole`,
  `forPermission`) denies anonymous, and `forAny`/`forAll` only compose those
- a constant must be the SOLE bare rule of its chain (`AuthRuleBuilder.kt:135-141`), so `public()`
  cannot be combined with anything
- `FloorAuthRuleBuilder` exposes no raw-rule hook — `add` is private — so no custom rule can be a floor
- a route-level rule ANDs AFTER the floor, so it can only ever narrow

**This does not change the implementation.** The equivalence is a property of today's DSL, not of the
design; an enumeration would need updating the moment it stops holding, and the evaluation cannot
disagree with the matrix because it IS the matrix's evaluator. But the claim "a custom rule kind is
handled" is now a statement about robustness, not about a case that exists — recorded in
`rest_fixtures.kt` next to where the fixture would have gone.

**Do NOT use `isSoleConstantChain()`** (`ApiRoute.kt:108`) — deliberately narrower, matching only a
single bare `public()`/`forbidden()`, so it misreports `public() and something`.

## Emitted shape

A second named function rather than an options object: the call body is a multi-line arrow, so
trailing arguments read badly, and the name states the fact at every site.

```ts
readonly listEvents = route('GET', '/funktor-conf/events', () => …)
readonly signIn = publicRoute('POST', '/auth/{realm}/signin', (params, body, options?) => …)
```

Both return `Route<F>`; `RouteRef` gains `readonly isPublic: boolean`.

`ApiAcl.canAccess` consults it before the matrix, so there is still exactly ONE lookup
implementation with a short-circuit for the case where a matrix cannot exist:

```ts
readonly canAccess = (route: RouteRef): boolean =>
    route.isPublic || this.canFullyAccess(route) || this.canPartiallyAccess(route)
```

**`canFullyAccess` must NOT gain the short-circuit.** It answers "does the caller-level rule chain
pass", and for a public route it does — but conflating the two would make the strict predicate lie
about a route that merely has no rules. Decide this explicitly when writing the test.

## Page routes are NOT this

A contributor DECLARES `requiresAuth` for a page route (`/insights`, `/login`) — it knows. Nothing is
derived. Only API-route publicness comes from the rules. Keeping the two separate is what stops the
router guard depending on a matrix that a logged-out visitor cannot fetch.

## Scope note for the red team

The bit only ever WIDENS what an anonymous client offers to try, never what the server accepts. The
failure direction is an affordance shown too eagerly → the call 401s. The scenarios worth attempting
are about staleness and derivation error, not bypass.

## Spec

- [x] `route.ts`: `RouteRef.isPublic`, `publicRoute()`, both returning `Route<F>`.
- [x] `acl.ts`: `canAccess` short-circuits on `isPublic`; `canFullyAccess` deliberately does not.
- [x] `TsClientSpec.Endpoint` carries `isPublic`; emitter picks the function AND narrows the import,
      so a client of one kind carries no dead import (a consuming app with `noUnusedLocals` cannot
      edit the file).
- [x] `RestApiTsContributor` computes it via `route.estimateAccess(User.anonymous)`.
- [x] Staleness: `--check` already covers it — confirmed, not rebuilt.

## Test evidence

- [x] Fixture groups for every BUILDABLE row: `public()`, `authenticated()`, `forRole("ops-admin")`,
      and `authenticated() and forRole("ops")`. The custom-rule row is unreachable — see the finding.
- [x] **`FxSplitSecuredRoutes` now floors `authenticated()` rather than `public()`**, so the
      merged-group fixture mirrors the real `funktor:auth` shape: two `ApiRoutes("login")` groups with
      DIFFERENT floors merging into one class. Publicness must survive the merge per-member, or
      `signIn` and `getMyApiAccess` would get the same answer. Asserted via the emitted import line.
- [x] `AclRuntimeParitySpec` derives the expected wrapper from the route's own chain rather than
      hardcoding it, and asserts the fixture set contains BOTH kinds — otherwise the wrapper choice
      would be untested.
- [x] ts-verify, 7 new checks: an anonymous `ApiAcl.empty` reaches a public route, is still denied
      everything else, `canFullyAccess` does NOT short-circuit, `getAccessLevel` still reports the raw
      matrix answer, and a public route present in the matrix is fully accessible.
- [x] **Mutation-tested, 5/5 killed**: derivation inverted; `canAccess` short-circuit removed;
      `canFullyAccess` widened with the short-circuit its KDoc forbids; the wrapper hardcoded to
      public; `publicRoute` setting the flag false.
- [x] Full test command(s) run + green: `./gradlew :ultra:codegen:check :funktor:codegen:check
      :funktor:rest:jvmTest` — 266 / 57 / 111, 0 failures. Compile sweep clean.

### Not yet done — the real-API confirmation

Nothing here has been run against the ACTUAL `funktor:auth` routes; the fixture mirrors their shape
but is not them. Regenerating the demo SDK is the natural check, and it is exactly the workflow the
maintainer described (regenerate, then test the app, before a deployment). Expect `signIn` to emit as
`publicRoute` and `getMyApiAccess` as `route`, in one merged `LoginApi`.

## Review record — /feature-review, 2026-08-02

Reviewed as part of the batch gate recorded in `.claude/tasks/20260731-sdk-auth-integration.md`
(base `887f9cd7` → `fb92a54c`). Fixes in `eb609f09`.

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | PASS | none against this feature |
| 2. Domain expert | PASS on the derivation, 1 MEDIUM on its docs | see below |
| 3. Security | PASS | derivation cannot over-report today |

**The derivation itself survived all three reviewers**, which is the part that mattered:
`isPublicToAnonymous` evaluates `route.estimateAccess(User.anonymous)` and both reviewers who probed
it reached the same conclusion by different routes. `FloorAuthRuleBuilder.build` throws on an empty
floor and `validateChain` enforces constant-soleness, so the predicate is true iff the group floor is
a sole `public()`. `ParamAutoRules.estimate` returns `Granted` and so cannot mask a denial. No
false-positive path found in either direction.

**Latent hazard recorded, not a finding:** `AuthRule.forCall`'s default
`estimateFn = { ApiAccessLevel.Granted }` (`AuthRule.kt:49`) means a future app-defined rule that
omits `estimateFn` estimates `Granted` for anonymous. Harmless only while the mandatory floor stands
— worth a comment at the definition if that ever becomes optional.

**One MEDIUM, fixed:** `acl.ts` documented `canFullyAccess`/`canPartiallyAccess`/`isDenied` as
mutually exclusive and exhaustive "UNCONDITIONALLY". `isPublic` breaks it — a public route absent
from the matrix reads false on all three, because only `canAccess` short-circuits. The natural
three-way render therefore puts every public route (sign-in, sign-up, recovery) in the
ownership-limited branch. The behaviour is right; the doc was wrong, and this is the one place the
vocabulary genuinely diverges from the Kotlin `ApiAcl`, which has no `isPublic`.

**Red-team follow-up:** folded into `.claude/tasks/20260802-redteam-sdk-auth.md` (section B) rather
than given its own file — the attack surface is the same matrix.
