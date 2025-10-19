package de.peekandpoke.funktor.auth.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiFeature
import de.peekandpoke.funktor.rest.ApiRoutes

class AuthApiFeature(converter: OutgoingConverter) : ApiFeature {

    data class RealmParam(
        val realm: String,
    )

    override val name = "Auth"

    override val description = """
        Endpoints for authentication.
    """.trimIndent()

    val auth = AuthApi(converter)

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        auth,
    )
}
