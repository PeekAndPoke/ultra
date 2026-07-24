package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.funktor.rest.ApiFeature

/** Api feature exposing the b2b org member-management endpoints. */
class B2bMembersApiFeature(
    services: B2bMembersServices,
) : ApiFeature {

    override val name = "B2B Members"

    override val description = """
        Member management for a b2b account admin's own organisation.
    """.trimIndent()

    val members = B2bMembersApi(services)

    override fun getRouteGroups() = listOf(members)
}
