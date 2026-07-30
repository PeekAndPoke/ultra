package io.peekandpoke.funktor.saas.api

import io.peekandpoke.funktor.rest.ApiFeature

/** Api feature exposing organisation CRUD (tenants + branches). */
class OrgsApiFeature : ApiFeature {

    override val name = "Funktor SaaS Organisations"

    override val description = """
        Super-user CRUD for organisations (tenants) and their branches.
    """.trimIndent()

    val orgs = OrgsApi()

    override fun getRouteGroups() = listOf(
        orgs
    )
}
