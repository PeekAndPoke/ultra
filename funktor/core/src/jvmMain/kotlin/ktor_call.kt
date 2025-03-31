package de.peekandpoke.funktor.core

import de.peekandpoke.ultra.kontainer.Kontainer
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun ApplicationRequest.fullUrl() = when (val port = origin.serverPort) {
    80 -> "${origin.scheme}://${host()}${path()}"
    else -> "${origin.scheme}://${host()}:${port}${path()}"
}

fun ApplicationRequest.methodAndUrl() = "${httpMethod.value} ${fullUrl()}"

fun ApplicationRequest.getReferer() = headers["Referer"] ?: headers["Referrer"]

fun Route.installKontainer(provider: (call: ApplicationCall) -> Kontainer) {
    val plugin = createRouteScopedPlugin("Install-Kontainer") {
        on(CallSetup) { call ->
            val kontainer = provider(call)

            call.attributes.provideKontainer(kontainer)
        }
    }

    install(plugin)
}
