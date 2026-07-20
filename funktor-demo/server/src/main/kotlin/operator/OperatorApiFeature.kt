package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.funktor.rest.ApiFeature

/** Api feature exposing the operator (platform super-user) console endpoints. */
class OperatorApiFeature : ApiFeature {

    override val name = "Funktor Demo Operators"

    override val description = """
        Platform super-user console: dashboard stats over all tenants.
    """.trimIndent()

    val operator = OperatorApi()

    override fun getRouteGroups() = listOf(
        operator
    )
}
