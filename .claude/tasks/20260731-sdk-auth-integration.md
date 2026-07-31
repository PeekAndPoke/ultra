# Ship auth through the SDK builder — the TypeScript counterpart of `AuthState`

**Status:** PLANNED — not started. Needs the decisions in §4 before code.
**Plan:** `.claude/tasks/20260729-ts-sdk-codegen.md` (the generator) and
`.claude/tasks/20260730-frontend-sdk-vue-contributors.md` (where contributors ship more than clients).
**Security-critical:** yes — sessions, tokens, JWT claims. Red-team follow-up required on completion.

## 1. Where this fits

**In the Vue-contributors family, and it should go FIRST there.**

Auth is the first contributor that ships *state and behaviour*, not just an API client. Everything the
frontend-SDK doc proposes — shared runtime modules, aggregation registries, components shipped from a
jar, a `requires(...)` check — is exercised by this one feature. Building it first turns that design
from a sketch into something with a user; building it after N other contributors means retrofitting.

It does **not** block, and is not blocked by, the remaining Phase-4 polish. Order does not matter
(maintainer, 2026-07-31).

## 2. What already exists, free

- **The auth API client is already generated.** The demo run emits `authClient.ts` from
  `AuthApiFeature` — `getRealm`, `signIn`, `selectOrg`, `signUp`, `activateAccount`,
  `resendActivation`, `setPassword`, the three `recoverAccount*` calls, `getMyApiAccess` and
  `refreshToken`. Nothing needs hand-writing at the API layer.
- **JWT injection is already the documented idiom.** `runtime/http.ts:50-58` says auth is a transport
  wrapper, and `SdkConfig` deliberately has no `headers` field so there is only one way to do it. The
  Kotlin side does exactly the same thing by another name: `AdminAppApis` installs a Ktor
  `defaultRequest` that reads a `tokenProvider()` closure
  (`funktor-demo/adminapp/src/jsMain/kotlin/api.kt:34-37`), wired to `State.auth().token?.token`.

So the gap is not the calls and not the header. **The gap is the session state machine.**

## 3. What `AuthState` actually does

`funktor/auth/src/jsMain/kotlin/AuthState.kt`, 488 lines. Read it before designing — this is a
summary, not a substitute.

| Concern | Detail |
|---|---|
| **Session data** | `Data<USER>` — token, realm, org, `tokenUserId`, `tokenExpires`, `claims`, the typed `user`, `permissions`. Reactive: `AuthState` *is* a `Stream<Data<USER>>` |
| **Lifecycle** | periodic expiry check, refresh before expiry, re-check on window focus, session-expired handling — all configurable via `AuthSessionConfig` (`checkIntervalMs`, `refreshBeforeExpiryMs`, `checkOnWindowFocus`, `onSessionExpired`, `onTokenRefreshed`) |
| **Flows** | login, logout, `selectOrg`, signup, activation, resend, the password-reset trio, `setPassword` |
| **JWT** | decodes claims client-side to derive `permissions`, `userId` (`sub`), expiry (`exp`) |
| **Routing** | `mount(builder)`, `routerMiddleWare(loginRoute)`, `redirectAfterLogin`, deep-link preservation |
| **Pending states** | `pendingOrgSelection`, `pendingActivation` — multi-step flows that survive a navigation |

## 4. Decisions needed before code

### 4.1 Storage — do NOT port what is there

`AuthState` persists the **whole** `Data<USER>` to `localStorage` under `"auth"`, and that is a
**known, filed security gap**: `.claude/tasks/20260719-token-storage-hardening.md` (status: DESIGN GAP,
security-critical). Any JS on the origin can read it, so an XSS exfiltrates the JWT and replays it
off-machine until expiry.

**Porting `AuthState` faithfully would re-create that defect in new code.** The TypeScript version must
take storage as an injected strategy, defaulting to **in-memory**, with localStorage available as an
explicit opt-in that names the trade-off. Whether the refresh-cookie design from that task lands first
is a sequencing question for the maintainer.

### 4.2 Framework-neutral core, thin Vue layer

`AuthState` is Kraft-coupled — it implements `Stream`, takes a `() -> Router`, and mounts routes. The
TypeScript version should split:

- `runtime/auth.ts` — the session store and lifecycle, depending on nothing but the generated auth
  client and a storage strategy. Testable in the node harness, usable from any framework.
- a Vue binding — `ref`/`computed` over the store, a router guard, `onScopeDispose` cleanup.

The alternative (one Vue-coupled class) is smaller today and wrong the first time anything else wants
it.

### 4.3 Generic over the user type

`AuthState<USER>` takes a `KSerializer<USER>`. The TypeScript equivalent takes a `z.ZodType<TUser>` —
which the generator already emits for whatever the app's user model is. This is the first real consumer
of generic emission outside `models.ts`.

### 4.4 Claims are NOT trustworthy, and the code must say so

`jwtClaims.kt:13` and `AuthState.kt:117` both warn that claims come from user-editable storage — a user
can hand-write `isSuperUser = true`. The client decodes them for UI decisions only; authorisation is
the server's. That warning must survive the port verbatim, at the point of use.

### 4.5 SSE intersects this

`.claude/tasks/20260731-sdk-sse-auth.md`: streams bypass the transport, so a transport-wrapper token
never reaches them. Whatever auth mechanism lands here must cover streams, or the two tasks must be
done together. Doing this one first without deciding that is how the asymmetry becomes permanent.

## 5. Spec (draft — do not implement before §4 is settled)

- [ ] `runtime/auth.ts`: session store, `AuthSessionConfig` equivalent, refresh lifecycle, injected
      storage strategy defaulting to memory.
- [ ] Transport wrapper helper, so the documented idiom is one call rather than a snippet to copy.
- [ ] Vue binding layer.
- [ ] `AuthTsContributor` shipping the runtime + bindings, conditional on the auth feature being reached.
- [ ] ts-verify: login → token attached → refresh-before-expiry → logout, against a stub transport.
- [ ] Red-team task on completion (`YYYYMMDD-redteam-sdk-auth.md`).

## 6. The other side — what the auth MODULE needs to provide

A separate plan, to be written. Open questions to seed it:

- **Is `AuthApiFeature` sufficient to drive every flow from a generated client?** The endpoint list
  looks complete, but `AuthState.getPasswordPolicy()` reads from somewhere — confirm it is on
  `AuthRealmModel` and therefore already on the wire.
- **Do `UserPermissions` / `AuthRealmModel` need TS claims**, or does the walk derive them correctly?
- **Does the refresh flow work without a cookie today**, and does the token-storage hardening change
  the contract the client codes against? If that work is coming, this client should be written against
  the *intended* contract, not the current one.
- **Realm selection**: `AuthApiClient` takes a `RealmId` at construction; the generated client does
  not. Decide whether realm is a client-construction concern or a per-call parameter.

## Test evidence

- [ ] Unit/behaviour tests
- [ ] ts-verify checks executing the session lifecycle
- [ ] Full test command(s) run + green: `...`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

**Red-team follow-up** (required — auth): `.claude/tasks/YYYYMMDD-redteam-sdk-auth.md`
