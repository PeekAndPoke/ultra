package io.peekandpoke.funktor.inspect.introspection.api

import kotlinx.serialization.Serializable

@Serializable
data class LifecycleHookInfo(
    val phase: String,
    val className: String,
    val executionOrder: Int,
)

@Serializable
data class ConfigInfoEntry(
    val key: String,
    val value: String,
)

@Serializable
data class CliCommandInfo(
    val name: String,
    val help: String,
)

@Serializable
data class FixtureInfo(
    val className: String,
    val dependsOn: List<String>,
)

@Serializable
data class RepairInfo(
    val className: String,
)

@Serializable
data class EndpointInfo(
    val feature: String,
    val group: String,
    val method: String,
    val uri: String,
    val authDescription: String,
)

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
data class AppLifecycleInfo(
    val startedAt: String,
    val uptimeSeconds: Long,
    val jvmVersion: String,
    val kotlinVersion: String,
)
