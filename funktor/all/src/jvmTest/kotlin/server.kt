package io.peekandpoke.funktor

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.installKontainer
import io.peekandpoke.funktor.core.jwtGenerator
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiStatusPages.installApiStatusPages
import io.peekandpoke.funktor.rest.auth.jwtUserProvider
import io.peekandpoke.funktor.rest.handle
import io.peekandpoke.ultra.security.jwt.JwtAnonymous

private const val AUTH_REALM_ID = "api.funktor.test"

@Suppress("unused")
fun Application.module() = testApp.module(this) { app, config, init ->

    val jwtGen = init.jwtGenerator

    authentication {
        jwt(AUTH_REALM_ID) {
            realm = "Funktor Test"
            verifier(jwtGen.verifier)
            validate { credential -> JWTPrincipal(credential.payload) }

            challenge { _, _ ->
                if (call.authentication.principal<JWTPrincipal>() == null) {
                    call.authentication.principal(
                        JWTPrincipal(JwtAnonymous(""))
                    )
                }
            }
        }
    }

    routing {
        host("api.*".toRegex()) {
            installKontainer { call ->
                app.kontainers.create {
                    with { call.jwtUserProvider() }
                }
            }

            installApiStatusPages()

            authenticate(AUTH_REALM_ID) {
                val allRoutes = init.getAll(ApiFeature::class)
                    .flatMap { it.getRouteGroups() }
                    .flatMap { it.all }

                allRoutes.forEach { handle(it) }
            }
        }
    }
}
