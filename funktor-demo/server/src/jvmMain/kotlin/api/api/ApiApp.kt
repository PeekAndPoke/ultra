package io.peekandpoke.funktor.demo.server.api.api

import de.peekandpoke.funktor.core.broker.fallback
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.core.methodAndUrl
import de.peekandpoke.funktor.rest.ApiFeature
import de.peekandpoke.funktor.rest.ApiStatusPages.installApiStatusPages
import de.peekandpoke.funktor.rest.apiRespond
import de.peekandpoke.funktor.rest.handle
import de.peekandpoke.ultra.security.jwt.JwtAnonymous
import de.peekandpoke.ultra.security.jwt.JwtGenerator
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*

class ApiApp(
    val features: List<ApiFeature>,
    private val auth: JwtGenerator,
    private val config: AppConfig,
) {
    companion object {
        const val AUTH_REALM_ID = "api.funktor-demo.io"
    }

    fun Application.configure() {
        authentication {
            jwt(AUTH_REALM_ID) {
                realm = "Funktor Demo"
                verifier(auth.verifier)
                validate { credential -> JWTPrincipal(credential.payload) }

                // NOTICE: When the JWT is invalid or not present we fall back to an anonymous user
                challenge { _, _ ->
                    if (call.authentication.principal<JWTPrincipal>() == null) {
                        call.authentication.principal(
                            JWTPrincipal(JwtAnonymous(""))
                        )
                    }
                }
            }
        }
    }

    fun Route.mountApiAppModule() {

        installApiStatusPages()

        // Add configurations to the application
        application.configure()

        route("/_/ping") {
            val handler: RoutingHandler = {
                call.apiRespond(call.request.methodAndUrl())
            }

            options(handler)
            head(handler)
            get(handler)
            put(handler)
            post(handler)
            delete(handler)
            patch(handler)
        }

        // Fallback for all routes that did not match
        fallback {
            throw NotFoundException()
        }

        authenticate(AUTH_REALM_ID) {
            val allRoutes = features.flatMap { it.getRouteGroups() }.flatMap { it.all }

            // mount all protected routes
            allRoutes.forEach {
                handle(it)
            }
        }
    }
}
