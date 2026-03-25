package io.peekandpoke.funktor.logging.api

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.rest.ApiFeature

/**
 * Api feature for UpNext
 */
class LoggingApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "Funktor Logging"

    override val description = """
        Exposes information about logs recorded by Funktor.
    """.trimIndent()

    /** The Api endpoints for the workers */
    val logging = LoggingApi(converter)

    override fun getRouteGroups() = listOf(
        logging
    )
}
