package io.peekandpoke.funktor.demo.server.operator

import io.peekandpoke.funktor.demo.common.operator.OperatorApiClient
import io.peekandpoke.funktor.demo.common.operator.OperatorDashboardStats
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.saas.domain.asApiModel
import io.peekandpoke.funktor.saas.funktorSaas
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.ultra.remote.ApiResponse

/** Platform super-user (operators realm) console API. */
class OperatorApi : ApiRoutes("operator") {

    val dashboardStats = OperatorApiClient.GetDashboardStats.mount {
        docs {
            name = "Operator dashboard stats"
        }.codeGen {
            funcName = "getDashboardStats"
        }.authorize {
            isSuperUser()
        }.handle {
            val orgs = funktorSaas.findAll().map { it.asApiModel() }

            val stats = OperatorDashboardStats(
                orgs = orgs.size,
                branches = orgs.sumOf { it.branches.size },
                activeOrgs = orgs.count { it.status == OrgStatus.Active },
                suspendedOrgs = orgs.count { it.status == OrgStatus.Suspended },
                archivedOrgs = orgs.count { it.status == OrgStatus.Archived },
                operators = operators.operatorUsersRepo.count().toInt(),
            )

            ApiResponse.ok(stats)
        }
    }
}
