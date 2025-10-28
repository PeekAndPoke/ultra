package de.peekandpoke.funktor.core.broker

import io.ktor.server.application.*
import io.ktor.server.response.*

/**
 * Helper to redirect to the referer
 */
suspend fun ApplicationCall.redirectToReferrer() {
    return respondRedirect(this.request.headers["Referer"] ?: "", false)
}

/**
 * Responds to a client with a `301 Moved Permanently` or `302 Found` redirect
 */
suspend fun <T : Any> ApplicationCall.respondRedirect(route: TypedRoute.Bound<T>, permanent: Boolean = false) {
    return respondRedirect(typedRouteRenderer.render(route), permanent)
}
