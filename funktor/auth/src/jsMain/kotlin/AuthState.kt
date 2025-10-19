package de.peekandpoke.funktor.auth

import de.peekandpoke.funktor.auth.api.AuthApiClient
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignInResponse
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.funktor.auth.model.PasswordPolicy
import de.peekandpoke.kraft.addons.decodeJwtAsMap
import de.peekandpoke.kraft.routing.Route
import de.peekandpoke.kraft.routing.Router
import de.peekandpoke.kraft.routing.routerMiddleware
import de.peekandpoke.ultra.security.user.UserPermissions
import de.peekandpoke.ultra.slumber.JsonUtil.toJsonObject
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import de.peekandpoke.ultra.streams.Unsubscribe
import de.peekandpoke.ultra.streams.ops.persistInLocalStorage
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import kotlin.js.Date

inline fun <reified USER> authState(
    api: AuthApiClient,
    noinline router: () -> Router,
) = AuthState<USER>(
    userSerializer = serializer(),
    api = api,
    router = router,
)

class AuthState<USER>(
    val userSerializer: KSerializer<USER>,
    val api: AuthApiClient,
    val router: () -> Router,
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

    override fun invoke(): Data<USER> = streamSource()

    override fun subscribeToStream(sub: (Data<USER>) -> Unit): Unsubscribe = streamSource.subscribeToStream(sub)

    fun getPasswordPolicy(): PasswordPolicy {
        return streamSource().realm?.passwordPolicy ?: PasswordPolicy.default
    }

    fun routerMiddleWare(loginRoute: Route.Bound) = routerMiddleware {
        val auth = invoke()

        val loginUri = router().strategy.render(loginRoute)

        if (!auth.isLoggedIn) {
            redirectAfterLoginUri = uri.takeIf { it != loginUri }
            redirectTo(loginUri)
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
        }

        return streamSource()
    }

    suspend fun recover(request: AuthRecoveryRequest): AuthRecoveryResponse? {
        val response = api
            .recover(request)
            .map { it.data }
            .catch { null }
            .firstOrNull()

        return response
    }

    fun logout() {
        streamSource(Data.empty())
    }

    suspend fun requestAuthUpdate(request: AuthUpdateRequest): Boolean {
        val auth = streamSource()

        if (!auth.isLoggedIn) return false

        val result = api.update(request)
            .catch { /* noop */ }
            .map { it.data!! }
            .firstOrNull()

        return result?.success == true
    }

    private fun readJwt(response: AuthSignInResponse, user: USER): Data<USER> {
        val claims = decodeJwtAsMap(response.token.token)

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
