package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.user
import io.peekandpoke.funktor.demo.common.showcase.AuthProviderInfo
import io.peekandpoke.funktor.demo.common.showcase.AuthRealmInfo
import io.peekandpoke.funktor.demo.common.showcase.AuthRuleCheckResult
import io.peekandpoke.funktor.demo.common.showcase.PasswordValidationResponse
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse

class AuthShowcaseApi(converter: OutgoingConverter) : ApiRoutes("showcase-auth", converter) {

    val getRealms = ShowcaseApiClient.GetAuthRealms.mount {
        docs {
            name = "List auth realms"
        }.codeGen {
            funcName = "getAuthRealms"
        }.authorize {
            public()
        }.handle {
            val realms = funktorAuth.realms.map { realm ->
                AuthRealmInfo(
                    id = realm.id,
                    providers = realm.providers.map { provider ->
                        AuthProviderInfo(
                            id = provider.id,
                            type = provider::class.simpleName ?: "?",
                            capabilities = provider.capabilities.map { it::class.simpleName ?: "?" },
                        )
                    },
                    passwordPolicyRegex = realm.passwordPolicy.regex,
                    passwordPolicyDescription = realm.passwordPolicy.description,
                )
            }

            ApiResponse.ok(realms)
        }
    }

    val validatePassword = ShowcaseApiClient.PostValidatePassword.mount {
        docs {
            name = "Validate password against policy"
        }.codeGen {
            funcName = "validatePassword"
        }.authorize {
            public()
        }.handle { body ->
            val realm = funktorAuth.realms.firstOrNull()
            val policy = realm?.passwordPolicy

            val matches = policy?.matches(body.password) ?: true

            ApiResponse.ok(
                PasswordValidationResponse(
                    matches = matches,
                    policyDescription = policy?.description ?: "No policy configured",
                )
            )
        }
    }

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
