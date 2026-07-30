# Frontend JWT Auto-Refresh & Session Lifecycle

**Status:** DONE (2026-04-06) — Implemented in the `funktor-apiacl-frontend-integration` branch.



## Commits & files changed

<!-- Generated 2026-07-28 from `git log --follow --name-status` over this task file.
     Commits that merely renamed the doc into the archive (R100) are excluded, since
     their code belongs to whatever task shipped alongside the move. -->

| Commit | Date | Files | Subject |
|---|---|---:|---|
| `8cfa70d1` | 2026-04-07 | 29 | api acl info for frontend test hardening |
| `1e8df5da` | 2026-04-05 | 1 | api acl info for frontend |

Archived by `d71db9f8`, `28973c5e` (rename only — that commit's code belongs to another task).

### Files changed (30)

**docs-site/src**
- `docs-site/src/components/HilbertBackground.astro`
- `docs-site/src/layouts/BaseLayout.astro`

**funktor-demo/adminapp**
- `funktor-demo/adminapp/src/jsMain/kotlin/index.kt`
- `funktor-demo/adminapp/src/jsMain/kotlin/pages/ProfilePage.kt`

**funktor/all**
- `funktor/all/src/jvmTest/kotlin/AuthApiSpec.kt`

**funktor/auth**
- `funktor/auth/src/commonMain/kotlin/api/AuthApiClient.kt`
- `funktor/auth/src/jsMain/kotlin/AuthSessionConfig.kt`
- `funktor/auth/src/jsMain/kotlin/AuthState.kt`
- `funktor/auth/src/jvmMain/kotlin/AuthRealm.kt`
- `funktor/auth/src/jvmMain/kotlin/AuthSystem.kt`
- `funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt`

**funktor/inspect**
- `funktor/inspect/src/jvmMain/kotlin/introspection/services/ApiAccessDescriptor.kt`

**funktor/rest**
- `funktor/rest/src/jvmMain/kotlin/auth/AuthRule.kt`
- `funktor/rest/src/jvmTest/kotlin/auth/AccessLevelCheckSpec.kt`

**ultra/html**
- `ultra/html/src/jvmTest/kotlin/CloudinaryImageSpec.kt`
- `ultra/html/src/jvmTest/kotlin/ImageSrcSetSpec.kt`
- `ultra/html/src/jvmTest/kotlin/PlaceholdersSpec.kt`

**ultra/semanticui**
- `ultra/semanticui/src/commonTest/kotlin/SemanticColorSpec.kt`
- `ultra/semanticui/src/commonTest/kotlin/SemanticFlagSpec.kt`
- `ultra/semanticui/src/commonTest/kotlin/SemanticNumberSpec.kt`

**ultra/vault**
- `ultra/vault/src/jvmTest/kotlin/DatabaseSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/RemoveResultSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/RepositoryHooksSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/SharedRepoClassLookupSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/SoftDeletableSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/TypedQuerySpec.kt`
- `ultra/vault/src/jvmTest/kotlin/VaultConfigSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/VaultExceptionSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/VaultModelsSpec.kt`
- `ultra/vault/src/jvmTest/kotlin/profiling/QueryProfilerSpec.kt`

## Context

Frontend SPAs using funktor/auth need robust session lifecycle handling:

1. **JWT expiry detection** — the frontend should know when the current JWT has expired without waiting
   for a 401 from the server.
2. **Periodic token refresh** — refresh the JWT while the user is active so they don't hit expiry mid-session.
3. **ACL refresh on token refresh** — when we refresh the JWT, also refresh the `ApiAcl` (permissions may
   have changed server-side, e.g. admin revoked a role).
4. **Focus-regain check** — when the browser window regains focus (user switches back from another tab),
   immediately check if the token is expired. If yes → redirect to login.
5. **Integration** — this lives inside `funktor/auth` frontend machinery that Kraft apps already use via
   `authState<USER>(...)`.

## Why this belongs to funktor/auth, not each Kraft app

Every consumer of funktor/auth faces the same requirements. Today each app would re-implement the
expiry check, the refresh timer, and the focus-regain handler. Pushing this into `AuthState` (or a
companion class) means:

- One canonical implementation, audited once
- Consistent UX across all funktor/auth-using apps
- Easy to extend with e.g. offline detection, refresh-failure backoff

## Pieces we'll reuse

- `WindowController.onWindowFocus` / `onWindowBlur` — already added to kraft/core, perfect for focus-regain check
- `AuthState<USER>` — already in `funktor/auth/src/jsMain/kotlin/AuthState.kt`, holds the current token
- `AuthState.Data.tokenExpires` — already tracks expiry timestamp (ISO string)
- `AuthState.jwtDecoder` — already injected (we refactored this during the jwtdecode migration)
- `ApiAcl` — will be available after the ApiAcl plan ships
- Kraft lifecycle hooks — for wiring into component lifecycle where needed
- `launch { }` + coroutine delay — for the periodic refresh timer

## High-level design sketch

```kotlin
class AuthState<USER>(
    // ... existing fields ...
    val jwtDecoder: (String) -> Map<String, Any?>,
    val refreshInterval: Duration = 5.minutes,
    val expiryGraceWindow: Duration = 30.seconds,
) : Stream<AuthState.Data<USER>> {

    /** True if the current token is expired or within the grace window. */
    fun isTokenExpiredOrExpiring(): Boolean

    /** Call the refresh-token endpoint; updates AuthState.Data and refreshes ApiAcl if available. */
    suspend fun refreshToken(): Data<USER>

    /** Starts the periodic-refresh coroutine. Safe to call multiple times (idempotent). */
    fun startAutoRefresh(scope: CoroutineScope)

    /** Stops auto-refresh (e.g. on logout). */
    fun stopAutoRefresh()

    /**
     * Checks expiry on window focus. If expired, triggers logout + navigation to login page.
     * Wire this up via `lifecycle { onWindowFocus { authState.checkOnFocus() } }` in a top-level component.
     */
    fun checkOnFocus(onExpired: () -> Unit)
}
```

## Open questions to answer when we implement

1. **Refresh endpoint** — does funktor/auth already expose a refresh-token endpoint? If not, we need to
   add one (server-side work).
2. **Race conditions** — if two focus events or two refresh timers fire concurrently, we must not
   double-refresh. Use a `Mutex` or a single-flight promise.
3. **Failure handling** — if refresh fails (network error, server 401), what's the behavior? Log out
   immediately, or retry with backoff?
4. **Clock skew** — the JWT's `exp` is server-side time. If client clock is off by minutes, grace
   window must absorb that. Compute expiry against server-reported time if available.
5. **Offline grace** — if the user is offline, should we keep them "logged in" locally until they
   come back online? Or immediately log out?
6. **Multi-tab** — two tabs open, both refresh at the same time → token invalidation race. Use
   `BroadcastChannel` or `storage` event for cross-tab coordination?
7. **ACL refresh timing** — refresh ACL with every token refresh, or only on sign-in / explicit
   request? (Trade-off: network cost vs. staleness.)

## Files likely affected

- `funktor/auth/src/jsMain/kotlin/AuthState.kt` — primary changes
- `funktor/auth/src/jsMain/kotlin/` (new file) — maybe a `SessionLifecycle` helper class
- `funktor/auth/src/commonMain/kotlin/` — if we need new API models for refresh
- Consumer apps using `authState<USER>` will opt in via `startAutoRefresh(scope)` and
  `lifecycle { onWindowFocus { authState.checkOnFocus(...) } }` — no mandatory migration

## Verification

- Unit tests for expiry calculation (with/without grace window)
- Integration test with fake clock: refresh fires at interval
- Manual browser test: open app, let token expire, switch away, switch back → redirected to login
- Cross-tab test: refresh in one tab, verify other tab sees the updated token

## Sequencing

**Do not start this until ApiAcl ships.** The token-refresh path wants to refresh the ACL too, so
having `ApiAcl` available as a concept simplifies the design.