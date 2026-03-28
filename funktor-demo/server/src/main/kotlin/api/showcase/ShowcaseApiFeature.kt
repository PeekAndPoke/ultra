package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes

class ShowcaseApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "Showcase"

    override val description = """
        Demonstrates all funktor features interactively.
    """.trimIndent()

    val core = CoreShowcaseApi(converter)
    val rest = RestShowcaseApi(converter)
    val auth = AuthShowcaseApi(converter)
    val cluster = ClusterShowcaseApi(converter)
    val messaging = MessagingShowcaseApi(converter)
    val sse = SseShowcaseApi(converter)
    val system = SystemShowcaseApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        core,
        rest,
        auth,
        cluster,
        messaging,
        sse,
        system,
    )
}
