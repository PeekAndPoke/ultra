package io.peekandpoke.funktor.inspect.introspection.services

import io.peekandpoke.funktor.auth.AuthRealm
import io.peekandpoke.funktor.inspect.introspection.api.ApiAccessMatrixModel
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.acl.UserApiAccessMatrix
import io.peekandpoke.funktor.rest.acl.UserApiAccessProvider
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.rest.security.ReflectivePathFinder.Companion.findAnnotatedElementPaths
import io.peekandpoke.funktor.rest.security.SensitiveData
import io.peekandpoke.funktor.rest.security.security
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions

class ApiAccessDescriptor(
    features: Lazy<List<ApiFeature>>,
    realms: Lazy<List<AuthRealm<*>>>,
) : UserApiAccessProvider {
    private val features: List<ApiFeature> by features
    private val realms: List<AuthRealm<*>> by realms

    override fun describeForUser(user: User): UserApiAccessMatrix {
        val entries = features.flatMap { feature ->
            feature.getRouteGroups().flatMap { group ->
                group.all.mapNotNull { route ->
                    val level = route.estimateAccess(user)
                    // Filter out Denied entries to avoid exposing the full API surface
                    if (level.isDenied()) {
                        null
                    } else {
                        UserApiAccessMatrix.Entry(
                        method = route.method.value,
                        uri = route.pattern.pattern,
                        level = level,
                    )
                    }
                }
            }
        }
        return UserApiAccessMatrix(entries = entries)
    }

    fun describe(): ApiAccessMatrixModel {

        val roles = collectKnownRoles()

        return ApiAccessMatrixModel(
            features = features.map { feature ->
                describeFeature(roles, feature)
            }
        )
    }

    private fun collectKnownRoles(): List<AuthRealm.KnownRole> {
        val fromRealms = realms.flatMap { it.getKnownRoles() }

        if (fromRealms.isNotEmpty()) {
            return fromRealms
        }

        return listOf(
            AuthRealm.KnownRole("SuperUser", UserPermissions(isSuperUser = true)),
            AuthRealm.KnownRole("Anonymous", UserPermissions()),
        )
    }

    private fun describeFeature(
        roles: List<AuthRealm.KnownRole>,
        feature: ApiFeature,
    ): ApiAccessMatrixModel.Feature {
        return ApiAccessMatrixModel.Feature(
            name = feature.name,
            groups = feature.getRouteGroups().map { group ->
                describeRouteGroup(roles, group)
            }
        )
    }

    private fun describeRouteGroup(
        roles: List<AuthRealm.KnownRole>,
        group: ApiRoutes,
    ): ApiAccessMatrixModel.Group {
        return ApiAccessMatrixModel.Group(
            name = group.name,
            endpoints = group.all.map { endpoint ->
                describeEndpoint(roles, endpoint)
            }
        )
    }

    private fun describeEndpoint(
        roles: List<AuthRealm.KnownRole>,
        endpoint: ApiRoute<*>,
    ): ApiAccessMatrixModel.Endpoint {

        val allowsSensitiveData = endpoint.security.allowsSensitiveData

        val sensitiveData = endpoint.responseType
            .findAnnotatedElementPaths {
                it.hasAnnotation<SensitiveData>()
            }.map {
                ApiAccessMatrixModel.SensitiveDataInfo(
                    path = "${it.path.joinToString(".")} (${it.type})",
                    isError = !allowsSensitiveData,
                )
            }

        return ApiAccessMatrixModel.Endpoint(
            uri = endpoint.pattern.pattern,
            name = endpoint.docs.name ?: "n/a",
            description = endpoint.docs.description ?: "",
            responseType = endpoint.responseType.type.toString(),
            allowsSensitiveData = allowsSensitiveData,
            sensitiveDataExposure = sensitiveData,
            roles = roles.map { role ->
                ApiAccessMatrixModel.AccessByRole(
                    role = role.name,
                    level = endpoint.estimateAccess(role.permissions),
                )
            }
        )
    }
}
