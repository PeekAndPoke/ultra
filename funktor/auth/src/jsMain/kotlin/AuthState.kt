package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import io.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
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
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonObject
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import org.w3c.dom.events.Event
import kotlin.js.Date

inline fun <reified USER> authState(
    frontend: AuthFrontend,
    api: AuthApiClient,
    noinline router: () -> Router,
    noinline jwtDecoder: (String) -> Map<String, Any?> = { emptyMap() },
    sessionConfig: AuthSessionConfig = AuthSessionConfig(),
) = AuthState<USER>(
    userSerializer = serializer(),
    frontend = frontend,
    api = api,
    router = router,
    jwtDecoder = jwtDecoder,
    sessionConfig = sessionConfig,
)

class AuthState<USER>(
    val userSerializer: KSerializer<USER>,
    val frontend: AuthFrontend,
    val api: AuthApiClient,
    val router: () -> Router,
    val jwtDecoder: (String) -> Map<String, Any?> = { emptyMap() },
    val sessionConfig: AuthSessionConfig = AuthSessionConfig(),
) : Stream<AuthState.Data<USER>> {

    @Serializable
    data class Data<USER>(
        val token: AuthSignInResponse.Token?,
        val realm: AuthRealmModel?,
        val tokenUserId: String?,
        val tokenExpires: String?,
        val claims: JsonObject?,
        val user: USER?,
        val permissions: UserPermissions,
    ) {
        companion object {
            fun <USER> empty() = Data<USER>(
                token = null,
                realm = null,
                tokenUserId = null,
                tokenExpires = null,
                claims = null,
                user = null,
                permissions = UserPermissions()
            )
        }

        val isLoggedIn get() = token != null && user != null

        val isNotLoggedIn get() = !isLoggedIn

        val loggedInUser get() = user.takeIf { isLoggedIn }
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
                val expiresMs = data.tokenExpires?.let { Date(it).getTime() }
                val nowMs = Date.now()

                if (expiresMs != null && nowMs >= expiresMs) {
                    // Persisted token is expired — clear it immediately
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

    suspend fun login(request: AuthSignInRequest): Data<USER> {
        val response = api
            .signIn(request)
            .map { it.data }
            .catch { streamSource(Data.empty()) }
            .firstOrNull()

        response?.let {
            val user = it.getTypedUser(userSerializer)
            val data = readJwt(response = it, user = user)

            streamSource(data)
            startSessionLifecycle()
        }

        return streamSource()
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
            .catch { /* noop */ }
            .map { it.data!! }
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

        val expiresMs = data.tokenExpires?.let { Date(it).getTime() } ?: return
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

                if (response != null) {
                    val user = response.getTypedUser(userSerializer)
                    val newData = readJwt(response = response, user = user)
                    streamSource(newData)
                    sessionConfig.onTokenRefreshed?.invoke()
                } else {
                    handleSessionExpired()
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

    // JWT parsing ////////////////////////////////////////////////////////////////////////////////

    private fun readJwt(response: AuthSignInResponse, user: USER): Data<USER> {
        val claims = jwtDecoder(response.token.token)

        // extract the permission from the token
        val permissions = response.token.permissionsNs.let { ns ->
            @Suppress("UNCHECKED_CAST")
            UserPermissions(
                organisations = (claims["$ns/organisations"] as? List<String> ?: emptyList()).toSet(),
                branches = (claims["$ns/branches"] as? List<String> ?: emptyList()).toSet(),
                groups = (claims["$ns/groups"] as? List<String> ?: emptyList()).toSet(),
                roles = (claims["$ns/roles"] as? List<String> ?: emptyList()).toSet(),
                permissions = (claims["$ns/permissions"] as? List<String> ?: emptyList()).toSet(),
            )
        }

        val expDate = (claims["exp"] as? Int)?.let { Date(it.toLong() * 1000) }

        val userId = claims["sub"] as? String ?: ""

        return Data(
            token = response.token,
            realm = response.realm,
            tokenUserId = userId,
            tokenExpires = expDate?.toISOString(),
            claims = claims.toJsonObject(),
            permissions = permissions,
            user = user
        )
    }
}
