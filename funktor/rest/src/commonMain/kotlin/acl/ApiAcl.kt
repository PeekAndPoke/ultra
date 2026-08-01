package io.peekandpoke.funktor.rest.acl

import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.TypedApiEndpoint

/**
 * Client-side API access control lookup.
 *
 * Built from a [UserApiAccessMatrix] (fetched from the server for the current user),
 * this class provides type-safe access checks using [TypedApiEndpoint] instances.
 *
 * **ADVISORY ONLY.** The server is the authority and nothing here enforces anything — this decides
 * what to RENDER, never what is allowed.
 *
 * Usage:
 * ```
 * val acl = ApiAcl(matrix)
 * if (acl.canAccess(MyApiClient.CreateItem)) { showButton() }
 * ```
 *
 * The predicates relate to each other in exactly two ways, and both matter when choosing one:
 *
 * - [canAccess] is the complement of [isDenied] — the only true opposite pair here.
 * - [canFullyAccess], [canPartiallyAccess] and [isDenied] are mutually exclusive and exhaustive,
 *   and [canAccess] is the union of the first two.
 *
 * The vocabulary is shared with the generated TypeScript SDK's `ApiAcl` (`ts/runtime/acl.ts`) so the
 * two clients cannot be read as meaning different things.
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
     * Worth showing at all — anything but [ApiAccessLevel.Denied].
     *
     * **Includes [ApiAccessLevel.Partial], so the server may still reject the call for specific
     * arguments.** That is the right default for visibility: a control that appears and then errors
     * explains itself, while one that silently never appears reads as a bug. For a DESTRUCTIVE
     * action, reach for [canFullyAccess] deliberately.
     */
    fun canAccess(endpoint: TypedApiEndpoint): Boolean {
        return !getAccessLevel(endpoint).isDenied()
    }

    /** Callable unconditionally, with no argument-dependent check. */
    fun canFullyAccess(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isGranted()
    }

    /**
     * Callable, but the server applies a further check on the ARGUMENTS — typically that an id in the
     * path is the caller's own, as with `/users/{id}/update`.
     *
     * The matrix cannot evaluate that; the caller must supply the equivalent rule:
     *
     * ```
     * val mayEdit = acl.canFullyAccess(UpdateUser) ||
     *         (acl.canPartiallyAccess(UpdateUser) && user.id == session.userId)
     * ```
     */
    fun canPartiallyAccess(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isPartial()
    }

    /** Never callable. */
    fun isDenied(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isDenied()
    }
}
