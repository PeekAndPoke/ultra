package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.kraft.routing.Route

data class AuthFrontendConfig(
    val redirectAfterLogin: Route.Bound,
    val backgroundImageUrl: String? = null,
)
