/**
 * The SDK, wired once.
 *
 * Everything here is a module-level SINGLETON on purpose. `AuthSession` holds the session and
 * `AclLoader` holds the access matrix, so a second instance of either means two disagreeing answers
 * to "who is signed in" — and the transport reads the session per request, so it must be the same
 * one the login form wrote to.
 *
 * The one rule worth stating: **build the transport ONCE and share the `SdkConfig`.** A client
 * constructed with a bare `fetchTransport()` sends no credentials, which does not fail loudly — it
 * gets a 401 that looks like a permissions problem.
 */
import { AclLoader } from './funktorsdk/runtime/acl-loader.ts'
import { AuthSession, authTransport } from './funktorsdk/runtime/auth.ts'
import { sdkConfig } from './funktorsdk/runtime/client.ts'
import { fetchTransport } from './funktorsdk/runtime/http.ts'
import { startAutoRefresh } from './funktorsdk/runtime/refresh.ts'
import { AuthClient } from './funktorsdk/authClient.ts'
import { FunktorInsightsClient } from './funktorsdk/funktorInsightsClient.ts'
import type { UserPermissions } from './funktorsdk/models.ts'

/**
 * The API is mounted behind a HOST matcher — `host("api.*".toRegex())` in the demo's `server.kt` —
 * so it is not served on plain localhost. The server's CORS list allows this app's dev origin.
 */
const API_BASE_URL = 'http://api.funktor-demo.localhost:36587'

/**
 * The realm this app signs in to.
 *
 * A CONSTANT because the demo is single-realm, and that is the whole reason `AuthSession` does not
 * carry the realm itself: `refreshToken({ realm })` closes over this. A multi-realm app would need
 * the session to remember which realm minted its token — an open question, deliberately not answered
 * by a single-realm demo.
 */
export const REALM = 'operators'

export const session = new AuthSession<UserPermissions>()

/**
 * Exported because CONTRIBUTED pages need it.
 *
 * A page shipped by a Kotlin module is constructed by the router, so nothing passes it a client — it
 * builds one from this, which `main.ts` provides to the app. See `runtime`-adjacent
 * `funktorsdk/ui/sdkContext.ts`.
 */
export const config = sdkConfig(API_BASE_URL, authTransport(fetchTransport(), session))

export const auth = new AuthClient(config)
export const insights = new FunktorInsightsClient(config)

/**
 * The access matrix, fetched per tab.
 *
 * Not persisted with the session: it is one entry per non-denied route, it belongs to the current
 * token, and `localStorage` is the wrong home for it. So opening a tab re-fetches, which is why the
 * `loading` state is real rather than theoretical — see [AclLoader].
 */
export const acl = new AclLoader(session, () => auth.login.getMyApiAccess(), {
    // Only fires when a load gives up with NOTHING to fall back on. A failure that still has a
    // previous matrix keeps the stale one and never lands here.
    onUnavailable: () => signOut(),
})

/**
 * Ends the session and drops the matrix.
 *
 * Both, always. A matrix outliving its session is a stale grant, and `AclLoader` cannot observe
 * `AuthSession` on its own.
 */
export function signOut(): void {
    session.signOut()
    acl.clear()
}

/** Loads the matrix if there is a session. Idempotent, so calling it on every route change is fine. */
export function ensureAcl(): void {
    if (session.state().isLoggedIn) acl.load()
}

// Refresh before expiry. Without this a session simply dies at `exp` and the user is "randomly
// logged out" with no error naming the cause.
startAutoRefresh(session, () => auth.login.refreshToken({ realm: REALM }), {
    onFailed: () => signOut(),
})

// A reload restores the session from storage but NOT the matrix, so ask for it on boot.
ensureAcl()
