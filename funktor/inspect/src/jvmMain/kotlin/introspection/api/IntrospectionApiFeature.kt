package io.peekandpoke.funktor.inspect.introspection.api

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.rest.ApiFeature

class IntrospectionApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "Funktor Introspection"

    override val description = """
        Exposes introspection information about the running application.
    """.trimIndent()

    val introspection = IntrospectionApi(converter)

    override fun getRouteGroups() = listOf(
        introspection
    )
}
