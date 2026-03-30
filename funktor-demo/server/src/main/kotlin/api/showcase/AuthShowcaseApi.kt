package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.core.user
import io.peekandpoke.funktor.demo.common.showcase.AuthRuleCheckResult
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

class AuthShowcaseApi : ApiRoutes("showcase-auth") {

    val getAuthRuleChecks = ShowcaseApiClient.GetAuthRuleChecks.mount {
        docs {
            name = "Check authorization rules for current user"
        }.codeGen {
            funcName = "getAuthRuleChecks"
        }.authorize {
            public()
        }.handle {
            val currentUser = try {
                user
            } catch (_: Exception) {
                null
            }

            val isSuperUser = currentUser?.permissions?.isSuperUser ?: false

            val results = listOf(
                AuthRuleCheckResult(
                    ruleName = "public()",
                    description = "Allows all authenticated users",
                    accessGranted = true,
                ),
                AuthRuleCheckResult(
                    ruleName = "isSuperUser()",
                    description = "Requires super user privileges",
                    accessGranted = isSuperUser,
                ),
                AuthRuleCheckResult(
                    ruleName = "forRole(\"admin\")",
                    description = "Requires 'admin' role",
                    accessGranted = currentUser?.permissions?.roles?.contains("admin") ?: false,
                ),
                AuthRuleCheckResult(
                    ruleName = "forGroup(\"staff\")",
                    description = "Requires 'staff' group membership",
                    accessGranted = currentUser?.permissions?.groups?.contains("staff") ?: false,
                ),
                AuthRuleCheckResult(
                    ruleName = "forPermission(\"manage_users\")",
                    description = "Requires 'manage_users' permission",
                    accessGranted = currentUser?.permissions?.permissions?.contains("manage_users") ?: false,
                ),
            )

            ApiResponse.ok(results)
        }
    }
}
