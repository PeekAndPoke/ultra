package io.peekandpoke.funktor.auth.api

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes

class AuthApiFeature : ApiFeature {

    data class RealmParam(
        val realm: String,
    )

    override val name = "Auth"

    override val description = """
        Endpoints for authentication.
    """.trimIndent()

    val auth = AuthApi()

    /** The authenticated self-service routes (set-password, refresh, my-api-access). */
    val authUser = AuthUserApi()

    override fun getRouteGroups(): List<ApiRoutes> = listOf(
        auth,
        authUser,
    )
}
