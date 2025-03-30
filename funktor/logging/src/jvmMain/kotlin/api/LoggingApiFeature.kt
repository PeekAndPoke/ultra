package de.peekandpoke.ktorfx.logging.api

import de.peekandpoke.ktorfx.core.broker.OutgoingConverter
import de.peekandpoke.ktorfx.rest.ApiFeature

/**
 * Api feature for UpNext
 */
class LoggingApiFeature(converter: OutgoingConverter) : ApiFeature {

    override val name = "KtorFx Logging"

    override val description = """
        Exposes information about logs recorded by KtorFX.
    """.trimIndent()

    /** The Api endpoints for the workers */
    val logging = LoggingApi(converter)

    override fun getRouteGroups() = listOf(
        logging
    )
}
