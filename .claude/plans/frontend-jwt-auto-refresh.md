# Frontend JWT Auto-Refresh & Session Lifecycle

**Status:** Deferred — dependent on ApiAcl landing first.

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