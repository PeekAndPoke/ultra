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
     * **Includes [ApiAccessLevel.Partial], and that is access.** `Partial` means the route is
     * callable and the server will additionally check the ARGUMENTS — typically "do you own this
     * resource". The user may use the route and its UI; enforcing the per-resource rule is the
     * backend's job.
     *
     * So this is the predicate to reach for, including for DESTRUCTIVE actions. Gating a delete on
     * [canFullyAccess] would hide it from the user on their OWN resource, which is the silent
     * disappearance this default exists to avoid.
     */
    fun canAccess(endpoint: TypedApiEndpoint): Boolean {
        return !getAccessLevel(endpoint).isDenied()
    }

    /**
     * Estimated [ApiAccessLevel.Granted]: no auth rule REPORTED a check on the arguments.
     *
     * Rarely what a view wants: use [canAccess] for "should this appear", destructive actions included.
     * Not proof that the route works for every resource — a `forCall` rule estimates `Granted` unless
     * it is given an `estimateFn`, and handler checks and route guards are invisible to the matrix.
     */
    fun canFullyAccess(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isGranted()
    }

    /**
     * Callable, but the server applies a further check on the ARGUMENTS — typically that an id in the
     * path is the caller's own, as with `/users/{id}/update`.
     *
     * Only a rule that estimates `Partial` produces it (`forCall(..., estimateFn = { Partial })`) — with
     * `forCall`'s default the route reads `Granted` and the first arm below admits everyone. The matrix
     * cannot evaluate the check; the caller must supply the equivalent rule:
     *
     * ```
     * val mayEdit = acl.canFullyAccess(UpdateUser) ||
     *         (acl.canPartiallyAccess(UpdateUser) && user.id == session.userId)
     * ```
     */
    fun canPartiallyAccess(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isPartial()
    }

    /** Not listed as callable for this user — absence from the matrix reads as denied. */
    fun isDenied(endpoint: TypedApiEndpoint): Boolean {
        return getAccessLevel(endpoint).isDenied()
    }
}
