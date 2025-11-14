package de.peekandpoke.funktor.auth

import de.peekandpoke.kraft.routing.Route

data class AuthFrontendConfig(
    val redirectAfterLogin: Route.Bound,
    val backgroundImageUrl: String? = null,
)
