package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes

class ShowcaseApiFeature : ApiFeature {

    override val name = "Showcase"

    override val description = """
        Demonstrates all funktor features interactively.
    """.trimIndent()

    val core = CoreShowcaseApi()
    val rest = RestShowcaseApi()
    val auth = AuthShowcaseApi()
    val cluster = ClusterShowcaseApi()
    val messaging = MessagingShowcaseApi()
    val sse = SseShowcaseApi()

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        core,
        rest,
        auth,
        cluster,
        messaging,
        sse,
    )
}
