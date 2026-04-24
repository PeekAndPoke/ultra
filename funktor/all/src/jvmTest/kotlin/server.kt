package io.peekandpoke.funktor

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.installKontainer
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiStatusPages.installApiStatusPages
import io.peekandpoke.funktor.rest.auth.anonymous
import io.peekandpoke.funktor.rest.auth.currentUserProvider
import io.peekandpoke.funktor.rest.auth.jwtCaller
import io.peekandpoke.funktor.rest.handle

private const val AUTH_JWT = "api.funktor.test.jwt"
private const val AUTH_ANON = "api.funktor.test.anon"

@Suppress("unused")
fun Application.module() = testApp.module(this) { app, config, init ->

    authentication {
        jwtCaller(AUTH_JWT, realm = "Funktor Test")
        anonymous(AUTH_ANON)
    }

    routing {
        host("api.*".toRegex()) {
            installKontainer { call ->
                app.kontainers.create {
                    with { call.currentUserProvider() }
                }
            }

            installApiStatusPages()

            authenticate(AUTH_JWT, AUTH_ANON) {
                val allRoutes = init.getAll(ApiFeature::class)
                    .flatMap { it.getRouteGroups() }
                    .flatMap { it.all }

                allRoutes.forEach { handle(it) }
            }
        }
    }
}
