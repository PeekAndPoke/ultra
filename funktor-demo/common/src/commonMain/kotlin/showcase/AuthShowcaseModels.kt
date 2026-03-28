package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class AuthRealmInfo(
    val id: String,
    val providers: List<AuthProviderInfo>,
    val passwordPolicyRegex: String,
    val passwordPolicyDescription: String,
)

@Serializable
data class AuthProviderInfo(
    val id: String,
    val type: String,
    val capabilities: List<String>,
)

@Serializable
data class PasswordValidationRequest(
    val password: String,
)

@Serializable
data class PasswordValidationResponse(
    val matches: Boolean,
    val policyDescription: String,
)

@Serializable
data class AuthRuleCheckResult(
    val ruleName: String,
    val description: String,
    val accessGranted: Boolean,
)
