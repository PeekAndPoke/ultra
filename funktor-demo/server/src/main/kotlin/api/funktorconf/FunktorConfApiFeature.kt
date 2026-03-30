package io.peekandpoke.funktor.demo.server.api.funktorconf

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.demo.server.funktorconf.FunktorConfServices
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes

class FunktorConfApiFeature(
    converter: OutgoingConverter,
    services: FunktorConfServices,
) : ApiFeature {

    override val name = "FunktorConf"

    override val description = """
        Conference management showcase — events, speakers, attendees.
    """.trimIndent()

    val conf = FunktorConfApi(converter, services)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        conf,
    )
}
