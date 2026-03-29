package io.peekandpoke.funktor.inspect.introspection.api

import io.peekandpoke.ultra.remote.ApiAccessLevel
import kotlinx.serialization.Serializable

@Serializable
data class ApiAccessMatrixModel(
    val features: List<Feature>,
) {
    @Serializable
    data class Feature(
        val name: String,
        val groups: List<Group>,
    )

    @Serializable
    data class Group(
        val name: String,
        val endpoints: List<Endpoint>,
    )

    @Serializable
    data class Endpoint(
        val uri: String,
        val name: String,
        val description: String,
        val responseType: String,
        val allowsSensitiveData: Boolean,
        val sensitiveDataExposure: List<SensitiveDataInfo>,
        val roles: List<AccessByRole>,
    ) {
        val hasProblems: Boolean
            get() = sensitiveDataExposure.any { it.isError }

        fun getAccessLevelOfRole(role: String): ApiAccessLevel {
            return roles.firstOrNull { it.role == role }?.level ?: ApiAccessLevel.Denied
        }
    }

    @Serializable
    data class AccessByRole(
        val role: String,
        val level: ApiAccessLevel,
    )

    @Serializable
    data class SensitiveDataInfo(
        val path: String,
        val isError: Boolean,
    )

    fun getAllRoles(): Set<String> {
        return features.flatMap { feature ->
            feature.groups.flatMap { group ->
                group.endpoints.flatMap { endpoint ->
                    endpoint.roles.map { it.role }
                }
            }
        }.toSet()
    }

    val allEndpoints: List<Endpoint> by lazy {
        features.flatMap { it.groups.flatMap { group -> group.endpoints } }
    }

    val numEndpointsWithErrors: Int by lazy {
        allEndpoints.count { it.hasProblems }
    }

    val numEndpointsAllowingSensitiveData: Int by lazy {
        allEndpoints.count { it.allowsSensitiveData }
    }
}
