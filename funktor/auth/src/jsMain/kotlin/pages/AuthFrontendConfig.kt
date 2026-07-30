package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.kraft.routing.Route
import kotlinx.html.FlowContent

data class AuthFrontendConfig(
    val redirectAfterLogin: Route.Bound,
    val backgroundImageUrl: String? = null,
    /** App name shown above the auth card (default chrome). */
    val title: String? = null,
    /** Logo image url shown above the auth card (default chrome). */
    val logoUrl: String? = null,
    /**
     * Custom header slot rendered above the auth card. When set it fully replaces [logoUrl] +
     * [title], so an app can brand the default chrome however it wants.
     */
    val header: (FlowContent.() -> Unit)? = null,
)
