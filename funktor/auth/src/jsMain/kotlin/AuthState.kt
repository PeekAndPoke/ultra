package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthOrgRef
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSelectOrgRequest
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.pages.AuthFrontend
import io.peekandpoke.kraft.routing.Route
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.routerMiddleware
import io.peekandpoke.kraft.utils.clearInterval
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.utils.setInterval
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import io.peekandpoke.ultra.streams.ops.persistInLocalStorage
import kotlinx.browser.window
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.w3c.dom.events.Event
import kotlin.js.Date

inline fun <reified USER> authState(
    frontend: AuthFrontend,
    api: AuthApiClient,
    noinline router: () -> Router,
    sessionConfig: AuthSessionConfig = AuthSessionConfig(),
) = AuthState<USER>(
    userSerializer = serializer(),
    frontend = frontend,
    api = api,
    router = router,
    sessionConfig = sessionConfig,
)

class AuthState<USER>(
    val userSerializer: KSerializer<USER>,
    val frontend: AuthFrontend,
    val api: AuthApiClient,
    val router: () -> Router,
    val sessionConfig: AuthSessionConfig = AuthSessionConfig(),
) : Stream<AuthState.Data<USER>> {

    @Serializable
    data class Data<USER>(
        val session: Session<USER>? = null,
    ) {
        /**
         * An authenticated session. Every field is present together, or there is no [session] at
         * all — so a half-logged-in state (a token without a user, etc.) is unrepresentable.
         */
        @Serializable
        data class Session<USER>(
            /** How this session is carried. One variant today; sealed so a second is additive. */
            val transport: AuthSignInResponse.Session,
            val realm: AuthRealmModel,
            /**
             * The organisation selected for this session, or null on an org-less realm.
             *
             * Comes from the sign-in / select-org / refresh response, NOT from the JWT: the token
             * carries only the org's id, and a display NAME is what a UI actually needs. Defaulted so
             * a session persisted before this field existed still decodes instead of logging the user
             * out.
             */
            val org: AuthOrgRef? = null,
            val tokenUserId: UserId?,
            val tokenExpires: MpInstant?,
            val user: USER,
            val permissions: UserPermissions,
        )

        companion object {
            fun <USER> empty() = Data<USER>(session = null)
        }

        val isLoggedIn get() = session != null

        val isNotLoggedIn get() = !isLoggedIn

        // Nullable pass-throughs: callers keep reading the same names; all null when logged out.
        /** The bearer token to attach to requests, or null when logged out. */
        val bearerToken
            get() = session?.transport?.let {
                when (it) {
                    is AuthSignInResponse.Session.Bearer -> it.token
                }
            }
        val realm get() = session?.realm
        val org get() = session?.org
        val tokenUserId get() = session?.tokenUserId
        val tokenExpires get() = session?.tokenExpires
        val user get() = session?.user

        /**
         * The session's permissions, DISPLAY-ONLY.
         *
         * Stated by the server rather than decoded from an unverified token, so they are at least
         * authentic — but the whole session is still persisted in user-editable localStorage, where a
         * user can hand-write `isSuperUser = true`. Use it to decide what the UI SHOWS, never what it
         * is allowed to do; `isSuperUser` in particular short-circuits every `has*` helper on
         * [UserPermissions]. Every real decision is re-derived server-side from the verified token.
         */
        val permissions get() = session?.permissions ?: UserPermissions()
    }

    private val streamSource = StreamSource<Data<USER>>(Data.empty())
        .persistInLocalStorage("auth", Data.serializer(userSerializer))

    private var redirectAfterLoginUri: String? = null

    // Session lifecycle state
    private var checkTimerId: Int? = null
    private var isRefreshing: Boolean = false
    private val windowFocusListener: (Event) -> Unit = { checkAndRefreshToken() }

    init {
        if (sessionConfig.enabled) {
            val data = streamSource()
            if (data.isLoggedIn) {
                val expiresMs = data.tokenExpires?.toEpochMillis()
                val nowMs = Date.now()

                if (expiresMs == null || nowMs >= expiresMs) {
                    // Expired, OR persisted before the claim decoder existed (no expiry recorded).
                    // The latter can never self-heal — `checkAndRefreshToken` returns early without an
                    // expiry, so it would never refresh and never repopulate permissions/org — so
                    // treat "unknown expiry" as stale and make the user re-authenticate once.
                    streamSource(Data.empty())
                } else {
                    // Token still valid — start lifecycle
                    startSessionLifecycle()
                }
            }
        }
    }

    override fun invoke(): Data<USER> = streamSource()

    override fun subscribeToStream(sub: (Data<USER>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    fun mount(builder: RouterBuilder) {
        frontend.mount(builder = builder, state = this)
    }

    fun getPasswordPolicy(): PasswordPolicy {
        return streamSource().realm?.passwordPolicy ?: PasswordPolicy.default
    }

    fun routerMiddleWare(loginRoute: Route.Bound) = routerMiddleware { ctx ->
        val auth = invoke()

        val loginUri = router().strategy.render(loginRoute)

        if (!auth.isLoggedIn) {
            redirectAfterLoginUri = ctx.uri.takeIf { it != loginUri }
            ctx.redirect(loginUri)
        } else {
            ctx.proceed()
        }
    }

    fun redirectAfterLogin(defaultUri: String) {
        val target = redirectAfterLoginUri ?: defaultUri

        router().navToUri(target)

        redirectAfterLoginUri = null
    }

    /**
     * When a sign-in resolves to multiple organisations, this holds the pending selection (the
     * selection token + the choices) until [selectOrg] is called. Null otherwise.
     */
    var pendingOrgSelection: AuthSignInResponse.OrgSelectionRequired? = null
        private set

    /** Cancels a pending multi-org selection (e.g. the user backs out of the picker). */
    fun clearPendingOrgSelection() {
        pendingOrgSelection = null
    }

    /**
     * What the activation page needs after a sign-in was refused for a not-yet-activated account.
     *
     * Carried in MEMORY rather than in the URL: the address would otherwise sit in the address bar,
     * the browser history and any outgoing `Referer`.
     */
    data class PendingActivation(
        val provider: String,
        val email: String,
        /** Single-use authorization for [resendActivation]. */
        val resendToken: String,
    )

    /**
     * Set when a sign-in resolved to [AuthSignInResponse.ActivationRequired] — the password was right
     * but the address is unproven. Null otherwise.
     */
    var pendingActivation: PendingActivation? = null
        private set

    /** Forgets a pending activation (e.g. the user navigates back to the login form). */
    fun clearPendingActivation() {
        pendingActivation = null
    }

    suspend fun login(request: AuthSignInRequest): Data<USER> {
        // Clear any selection left over from a previous attempt BEFORE this one runs. Otherwise a
        // failed login would leave the earlier attempt's still-valid selection token in place, and
        // the UI (which branches on pendingOrgSelection) would resurface that user's org picker —
        // letting a bystander whose own login just failed complete a sign-in as the earlier user.
        pendingOrgSelection = null
        // Same reasoning for the activation carrier: a stale one would send the NEXT person who fails
        // to sign in to an activation page prefilled with the previous user's address.
        pendingActivation = null

        val response = api
            .signIn(request)
            .map { it.data }
            .catch { streamSource(Data.empty()) }
            .firstOrNull()

        when (response) {
            is AuthSignInResponse.Success -> applySuccess(response)
            is AuthSignInResponse.OrgSelectionRequired -> pendingOrgSelection = response

            is AuthSignInResponse.ActivationRequired -> {
                // The address comes from what the user just typed, not from the response — the server
                // has no reason to echo it back.
                pendingActivation = PendingActivation(
                    provider = request.provider,
                    email = (request as? AuthSignInRequest.EmailAndPassword)?.email ?: "",
                    resendToken = response.resendToken,
                )
            }

            null -> { /* sign-in failed */ }
        }

        return streamSource()
    }

    suspend fun activateAccount(request: AuthActivateAccountRequest): AuthActivateAccountResponse? {
        return api
            .activateAccount(request)
            .map { it.data!! }
            .catch { /* noop */ }
            .firstOrNull()
    }

    suspend fun resendActivation(request: AuthResendActivationRequest): AuthResendActivationResponse? {
        return api
            .resendActivation(request)
            .map { it.data!! }
            .catch { /* noop */ }
            .firstOrNull()
    }

    /**
     * Completes a multi-org sign-in by choosing [orgId] against the [pendingOrgSelection] token.
     */
    suspend fun selectOrg(orgId: OrgId): Data<USER> {
        val pending = pendingOrgSelection ?: return streamSource()

        val response = api
            .selectOrg(AuthSelectOrgRequest(selectionToken = pending.selectionToken, orgId = orgId))
            .map { it.data }
            .catch { streamSource(Data.empty()) }
            .firstOrNull()

        if (response is AuthSignInResponse.Success) {
            applySuccess(response)
        }

        return streamSource()
    }

    private fun applySuccess(response: AuthSignInResponse.Success) {
        pendingOrgSelection = null
        val user = response.getTypedUser(userSerializer)
        val data = readSession(response = response, user = user)

        streamSource(data)
        startSessionLifecycle()
    }

    suspend fun recoverAccountInitPasswordReset(
        request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset? {
        val response = api
            .recoverAccountInitPasswordReset(request)
            .map { it.data!! }
            .catch { /* noop */ }
            .firstOrNull()

        return response
    }

    suspend fun recoverAccountValidatePasswordResetToken(
        request: AuthRecoverAccountRequest.ValidatePasswordResetToken,
    ): AuthRecoverAccountResponse.ValidatePasswordResetToken? {
        val response = api
            .recoverAccountValidatePasswordResetToken(request)
            .map { it.data!! }
            .catch { /* noop */ }
            .firstOrNull()

        return response
    }

    suspend fun recoverAccountSetPasswordWithToken(
        request: AuthRecoverAccountRequest.SetPasswordWithToken,
    ): AuthRecoverAccountResponse.SetPasswordWithToken? {
        val response = api
            .recoverAccountSetPasswordWithToken(request)
            .map { it.data!! }
            .catch { /* noop */ }
            .firstOrNull()

        return response
    }

    fun logout() {
        stopSessionLifecycle()
        streamSource(Data.empty())
    }

    suspend fun requestSetPassword(request: AuthSetPasswordRequest): Boolean {
        val auth = streamSource()

        if (auth.isNotLoggedIn) return false

        val result = api.setPassword(request)
            .map { it.data!! }
            .catch { /* noop */ }
            .firstOrNull()

        return result?.success == true
    }

    // Session lifecycle ////////////////////////////////////////////////////////////////////////////

    private fun startSessionLifecycle() {
        if (!sessionConfig.enabled) return
        stopSessionLifecycle()

        checkTimerId = setInterval(sessionConfig.checkIntervalMs) {
            checkAndRefreshToken()
        }

        if (sessionConfig.checkOnWindowFocus) {
            window.addEventListener("focus", windowFocusListener)
        }
    }

    private fun stopSessionLifecycle() {
        checkTimerId?.let { clearInterval(it) }
        checkTimerId = null
        window.removeEventListener("focus", windowFocusListener)
    }

    private fun checkAndRefreshToken() {
        val data = streamSource()
        if (data.isNotLoggedIn || isRefreshing) return

        val expiresMs = data.tokenExpires?.toEpochMillis() ?: return
        val nowMs = Date.now()

        when {
            nowMs >= expiresMs -> handleSessionExpired()
            (expiresMs - nowMs) <= sessionConfig.refreshBeforeExpiryMs -> doRefreshToken()
        }
    }

    private fun doRefreshToken() {
        if (isRefreshing) return
        isRefreshing = true

        launch {
            try {
                val response = api.refreshToken()
                    .map { it.data }
                    .catch { emit(null) }
                    .firstOrNull()

                if (response is AuthSignInResponse.Success) {
                    val user = response.getTypedUser(userSerializer)
                    val newData = readSession(response = response, user = user)
                    streamSource(newData)
                    sessionConfig.onTokenRefreshed?.invoke()
                } else {
                    // A refresh runs `refreshBeforeExpiryMs` BEFORE the token expires, so a failure
                    // here is usually transient — a network blip or a 502. Expiring the session
                    // immediately would log the user out while their token is still perfectly valid.
                    // Leave the session alone; the next tick retries, and `checkAndRefreshToken`
                    // expires it for real once `nowMs >= expiresMs`.
                    console.warn("[AuthState] token refresh failed; will retry on the next check")
                }
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun handleSessionExpired() {
        stopSessionLifecycle()

        val customHandler = sessionConfig.onSessionExpired
        if (customHandler != null) {
            customHandler()
            return
        }

        // Default: save current page for redirect-after-login, logout, navigate to login
        val currentUri = window.location.let { it.pathname + it.search + it.hash }
        val loginUri = router().strategy.render(frontend.routes.login())
        if (currentUri != loginUri) {
            redirectAfterLoginUri = currentUri
        }
        logout()
        router().navToUri(loginUri)
    }

}

/**
 * Maps a sign-in / refresh response into session state.
 *
 * **Nothing here reads the token.** Permissions, expiry and the user id are all stated by the server. It
 * replaced a client-side JWT decode that pulled the same three values out of unverified claims, plus a
 * raw claim map nothing consumed — worth not doing regardless of transport, since those claims came from
 * a blob the user can rewrite in devtools.
 *
 * Top-level and `internal` rather than a private member: it uses no state from [AuthState], and lifting
 * it out is what lets `AuthStateSessionMappingSpec` pin the decoupling directly.
 */
internal fun <USER> readSession(response: AuthSignInResponse.Success, user: USER): AuthState.Data<USER> {
    return AuthState.Data(
        session = AuthState.Data.Session(
            transport = response.session,
            realm = response.realm,
            org = response.org,
            tokenUserId = response.userId,
            tokenExpires = response.expiresAt,
            permissions = response.permissions,
            user = user,
        )
    )
}
