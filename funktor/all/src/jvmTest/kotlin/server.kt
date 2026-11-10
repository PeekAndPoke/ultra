package io.peekandpoke.funktor

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.routing.Route
import io.ktor.server.routing.host
import io.ktor.server.routing.routing
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.core.installKontainer
import io.peekandpoke.funktor.insights.instrumentWithInsights
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiStatusPages.installApiStatusPages
import io.peekandpoke.funktor.rest.auth.anonymous
import io.peekandpoke.funktor.rest.auth.currentUserProvider
import io.peekandpoke.funktor.rest.auth.jwtCaller
import io.peekandpoke.funktor.rest.handle
import io.peekandpoke.ultra.kontainer.Kontainer

private const val AUTH_JWT = "api.funktor.test.jwt"
private const val AUTH_ANON = "api.funktor.test.anon"

@Suppress("unused")
fun Application.module() = testApp.module(this) { app, _, init ->

    authentication {
        jwtCaller(AUTH_JWT, realm = "Funktor Test")
        anonymous(AUTH_ANON)
    }

    routing {
        host("api.*".toRegex()) {
            mountApi(app, init, record = false)
        }

        // The identical API surface, RECORDING -- see `insightsApp`. The app's insights switch is on for the whole
        // suite (`funktor.insights.enabled`, ULTRA-00301), so the insights API answers on both hosts; only this one
        // instruments its routes, so only this one writes records. Note this isolates the record WRITING; the
        // routing tracer it installs is application-global either way.
        host("insights.*".toRegex()) {
            mountApi(app, init, record = true)
        }
    }
}

/** Mounts every registered [ApiFeature]'s routes, recording insights when [record] is set and the switch is on. */
private fun Route.mountApi(
    app: App<FunktorAllTestConfig>,
    init: Kontainer,
    record: Boolean,
) {
    installKontainer { call ->
        app.kontainers.create {
            with { call.currentUserProvider() }
        }
    }

    installApiStatusPages()

    if (record) instrumentWithInsights(app)

    authenticate(AUTH_JWT, AUTH_ANON) {
        val allRoutes = init.getAll(ApiFeature::class)
            .flatMap { it.getRouteGroups() }
            .flatMap { it.all }

        allRoutes.forEach { handle(it) }
    }
}
