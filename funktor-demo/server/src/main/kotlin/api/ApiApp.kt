package io.peekandpoke.funktor.demo.server.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.appInfo
import io.peekandpoke.funktor.core.broker.fallback
import io.peekandpoke.funktor.core.methodAndUrl
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiStatusPages.installApiStatusPages
import io.peekandpoke.funktor.rest.apiRespond
import io.peekandpoke.funktor.rest.auth.anonymous
import io.peekandpoke.funktor.rest.auth.jwtCaller
import io.peekandpoke.funktor.rest.handle
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.delay
import kotlin.random.Random

class ApiApp(val features: List<ApiFeature>) {
    companion object {
        const val AUTH_JWT = "api.funktor-demo.io.jwt"
        const val AUTH_ANON = "api.funktor-demo.io.anon"
    }

    fun Application.configure() {
        authentication {
            jwtCaller(AUTH_JWT, realm = "Funktor Demo")
            // Terminal fallback so requests without a JWT are admitted as anonymous.
            anonymous(AUTH_ANON)
        }
    }

    fun Route.mountApiAppModule() {

        installApiStatusPages()

        // Add configurations to the application
        application.configure()

        route("/_/ping") {
            val handler: RoutingHandler = {
                val pong = ApiResponse.ok(
                    call.request.methodAndUrl()
                )

                call.apiRespond(pong)
            }

            options(handler)
            head(handler)
            get(handler)
            put(handler)
            post(handler)
            delete(handler)
            patch(handler)
        }

        get("/_/appinfo") {
            val result = appInfo

            call.apiRespond(result)
        }

        // Fallback for all routes that did not match
        fallback {
            delay(Random.nextLong(100, 300))
            throw NotFoundException()
        }

        authenticate(AUTH_JWT, AUTH_ANON) {
            val allRoutes = features.flatMap { it.getRouteGroups() }.flatMap { it.all }

            // mount all protected routes
            allRoutes.forEach {
                handle(it)
            }
        }
    }
}
