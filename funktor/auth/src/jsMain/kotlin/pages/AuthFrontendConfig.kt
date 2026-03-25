package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.kraft.routing.Route

data class AuthFrontendConfig(
    val redirectAfterLogin: Route.Bound,
    val backgroundImageUrl: String? = null,
)
