package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.funktor.demo.common.OperatorUserModel
import io.peekandpoke.funktor.demo.common.operator.OperatorApiClient
import io.peekandpoke.funktor.demo.common.operator.OperatorDashboardStats
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.saas.funktorSaas
import io.peekandpoke.ultra.remote.ApiResponse

/** Platform super-user (operators realm) console API. */
class OperatorApi : ApiRoutes("operator") {

    val dashboardStats = OperatorApiClient.GetDashboardStats.mount {
        docs {
            name = "Operator dashboard stats"
        }.codeGen {
            funcName = "getDashboardStats"
        }.authorize {
            // All realms share one JWT signing key, and the admin realm also mints super-user
            // tokens — the user-type claim is what scopes this console to the operators realm.
            isSuperUser()
            forUserType(OperatorUserModel.USER_TYPE)
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
