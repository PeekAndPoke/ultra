package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthFrontendRoutes
import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.kraft.routing.RouterBuilder

interface AuthFrontend {
    companion object {
        fun default(
            config: AuthFrontendConfig,
            routes: AuthFrontendRoutes = AuthFrontendRoutes(),
        ) = AuthFrontendDefault(config, routes)
    }

    val config: AuthFrontendConfig
    val routes: AuthFrontendRoutes

    fun <USER> mount(builder: RouterBuilder, state: AuthState<USER>)
}

class AuthFrontendDefault(
    override val config: AuthFrontendConfig,
    override val routes: AuthFrontendRoutes = AuthFrontendRoutes(),
) : AuthFrontend {

    override fun <USER> mount(builder: RouterBuilder, state: AuthState<USER>) {
        with(builder) {
            mount(routes.login) {
                LoginPage(state = state)
            }

            mount(routes.resetPassword) {
                ResetPasswordPage(
                    state = state,
                    provider = it[AuthFrontendRoutes.PROVIDER_PARAM],
                    token = it[AuthFrontendRoutes.TOKEN_PARAM],
                )
            }
        }
    }
}
