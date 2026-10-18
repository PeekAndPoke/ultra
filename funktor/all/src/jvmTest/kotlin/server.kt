package io.peekandpoke.funktor

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.peekandpoke.funktor.core.App
import io.peekandpoke.funktor.core.installKontainer
import io.peekandpoke.funktor.core.model.InsightsConfig
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
            mountApi(app, init, insights = null)
        }

        // The identical API surface with insights recording ON — see `insightsApp`. Two mounts rather
        // than one flag because `instrumentWithInsights` decides at INSTALL time, so a single mount
        // would either record for every spec in this module or for none. Note this isolates the
        // record WRITING; the routing tracer it installs is application-global either way.
        host("insights.*".toRegex()) {
            mountApi(app, init, insights = InsightsConfig(enabled = true))
        }
    }
}

/** Mounts every registered [ApiFeature]'s routes, recording insights when [insights] is enabled. */
private fun Route.mountApi(
    app: App<FunktorAllTestConfig>,
    init: Kontainer,
    insights: InsightsConfig?,
) {
    installKontainer { call ->
        app.kontainers.create {
            with { call.currentUserProvider() }
            insights?.let { with { it } }
        }
    }

    installApiStatusPages()

    instrumentWithInsights(insights)

    authenticate(AUTH_JWT, AUTH_ANON) {
        val allRoutes = init.getAll(ApiFeature::class)
            .flatMap { it.getRouteGroups() }
            .flatMap { it.all }

        allRoutes.forEach { handle(it) }
    }
}
