package io.peekandpoke.funktor.auth.api

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes

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
