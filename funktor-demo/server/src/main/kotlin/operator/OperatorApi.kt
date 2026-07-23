package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.funktor.demo.common.OperatorUserModel
import io.peekandpoke.funktor.demo.common.operator.OperatorApiClient
import io.peekandpoke.funktor.demo.common.operator.OperatorDashboardStats
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.saas.funktorSaas
import io.peekandpoke.ultra.remote.ApiResponse

/**
 * Platform super-user (operators realm) console API.
 *
 * Floor: super-user AND operators-realm. All realms share one JWT signing key and the admin realm
 * also mints super-user tokens, so the user-type claim is what scopes this whole console to the
 * operators realm — declared once as the group floor, inherited by every route.
 */
class OperatorApi : ApiRoutes(
    name = "operator",
    authFloor = {
        isSuperUser()
        forUserType(OperatorUserModel.USER_TYPE)
    },
) {

    val dashboardStats = OperatorApiClient.GetDashboardStats.mount {
        docs {
            name = "Operator dashboard stats"
        }.codeGen {
            funcName = "getDashboardStats"
        }.handle {
            val orgs = funktorSaas.findAll().map { it.value() }

            val stats = OperatorDashboardStats(
                orgs = orgs.size,
                branches = orgs.sumOf { it.branches.size },
                orgsByStatus = orgs.groupingBy { it.status.name }.eachCount(),
                operators = operators.operatorUsersRepo.count().toInt(),
            )

            ApiResponse.ok(stats)
        }
    }
}
