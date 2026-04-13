package io.peekandpoke.funktor.core

import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.peekandpoke.ultra.kontainer.Kontainer

/** Returns the full request URL including scheme, host, port (if non-80), and path. */
fun ApplicationRequest.fullUrl() = when (val port = origin.serverPort) {
    80 -> "${origin.scheme}://${host()}${path()}"
    else -> "${origin.scheme}://${host()}:${port}${path()}"
}

/** Returns the HTTP method and full URL as a single string (e.g. "GET https://..."). */
fun ApplicationRequest.methodAndUrl() = "${httpMethod.value} ${fullUrl()}"

/** Returns the Referer header value, trying both "Referer" and "Referrer" spellings. */
fun ApplicationRequest.getReferer() = headers["Referer"] ?: headers["Referrer"]

/** Installs a route-scoped plugin that provides a per-call [Kontainer] via [provideKontainer]. */
fun Route.installKontainer(provider: (call: ApplicationCall) -> Kontainer) {
    val plugin = createRouteScopedPlugin("Install-Kontainer") {
        on(CallSetup) { call ->
            val kontainer = provider(call)

            call.attributes.provideKontainer(kontainer)
        }
    }

    install(plugin)
}
