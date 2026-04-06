package io.peekandpoke.funktor.rest.acl

import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.TypedApiEndpoint

/**
 * Client-side API access control lookup.
 *
 * Built from a [UserApiAccessMatrix] (fetched from the server for the current user),
 * this class provides type-safe access checks using [TypedApiEndpoint] instances.
 *
 * Usage:
 * ```
 * val acl = ApiAcl(matrix)
 * if (acl.hasAccessTo(MyApiClient.CreateItem)) { /* show button */ }
 * ```
 */
class ApiAcl(matrix: UserApiAccessMatrix) {

    companion object {
        /** An empty ACL that denies access to all endpoints. */
        val empty = ApiAcl(UserApiAccessMatrix(entries = emptyList()))

        /** Builds the internal lookup key from method and uri. */
        private fun key(method: String, uri: String): String = "$method|$uri"
    }

    private val lookup: Map<String, ApiAccessLevel> = matrix.entries.associate { entry ->
        key(entry.method, entry.uri) to entry.level
    }

    /**
     * Returns the estimated [ApiAccessLevel] for the given [endpoint].
     *
     * Returns [ApiAccessLevel.Denied] if the endpoint is not present in the matrix
     * (secure-by-default).
     */
    fun getAccessLevel(endpoint: TypedApiEndpoint): ApiAccessLevel {
        return lookup[key(endpoint.httpMethod, endpoint.uri)] ?: ApiAccessLevel.Denied
    }

    /**
     * Returns `true` if the given [endpoint] has [ApiAccessLevel.Granted] access.
     *
     * Use this for strict access checks where partial access is not sufficient.
     */
    fun hasAccessTo(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isGranted()
    }

    /**
     * Returns `true` if the given [endpoint] has any access (not [ApiAccessLevel.Denied]).
     *
     * Use this when partial access is acceptable (e.g., showing a page with some
     * fields redacted).
     */
    fun hasAnyAccessTo(endpoint: TypedApiEndpoint): Boolean {
        return !getAccessLevel(endpoint).isDenied()
    }
}
