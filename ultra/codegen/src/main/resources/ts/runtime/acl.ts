/**
 * Client-side API access lookup — the TypeScript counterpart of `ApiAcl`
 * (`funktor/rest/src/commonMain/kotlin/acl/ApiAcl.kt`).
 *
 * Built from the matrix `AuthUserApi.getMyApiAccess` returns for the current user, it answers "may
 * this user call this route?" so a frontend can show a button, hide it, or render read-only.
 *
 * **ADVISORY ONLY.** The server is the authority and nothing here enforces anything — this decides
 * what to RENDER, never what is allowed. Treating it as enforcement is the one way to misuse it.
 *
 * HAND-WRITTEN AND CHECKED IN. The types below mirror `UserApiAccessMatrix` and `ApiAccessLevel`
 * structurally rather than importing them from `models.ts`: the runtime must not depend on generated
 * output, and those declarations only exist in an SDK that reached the auth feature.
 * `AclRuntimeParitySpec` (in `funktor:codegen`) fails if the Kotlin enum, the key format or the
 * strings a generated member carries drift from this file.
 */
import type { RouteRef } from './route.ts'

/** Mirrors `ApiAccessLevel` (`ultra/remote/src/commonMain/kotlin/ApiAccessLevel.kt`). */
export type ApiAccessLevel = 'Granted' | 'Partial' | 'Denied'

/** One row of the matrix. `uri` is the route pattern, placeholders included. */
export interface AccessMatrixEntry {
    readonly method: string
    readonly uri: string
    readonly level: ApiAccessLevel
}

/**
 * The matrix as the server sends it.
 *
 * Structural, so the generated `UserApiAccessMatrix` satisfies it without an import.
 */
export interface AccessMatrix {
    readonly entries: readonly AccessMatrixEntry[]
}

/**
 * Access lookup over a fetched matrix.
 *
 * The predicates relate to each other in exactly two ways, and both are worth knowing before
 * choosing one:
 *
 * - [canAccess] is the complement of [isDenied] — the only true opposite pair here.
 * - [canFullyAccess], [canPartiallyAccess] and [isDenied] are mutually exclusive and exhaustive,
 *   and [canAccess] is the union of the first two.
 *
 * Methods are arrow-function FIELDS, not prototype methods, so `const { canAccess } = acl` works.
 */
export class ApiAcl {
    private readonly lookup: Map<string, ApiAccessLevel>

    constructor(matrix: AccessMatrix) {
        this.lookup = new Map(matrix.entries.map((e) => [`${e.method}|${e.uri}`, e.level]))
    }

    /** Denies everything. What a logged-out visitor gets, since the matrix endpoint needs a session. */
    static readonly empty: ApiAcl = new ApiAcl({ entries: [] })

    /**
     * The level for [route], or `Denied` when the matrix has no row for it.
     *
     * **The fallback is the mechanism, not padding.** The server OMITS denied entries so the matrix
     * does not disclose the full API surface (`ApiAccessDescriptor.kt:28-30`), so absence is exactly
     * how denial is transmitted. Removing the fallback would grant everything unknown.
     */
    readonly getAccessLevel = (route: RouteRef): ApiAccessLevel =>
        this.lookup.get(`${route.method}|${route.uri}`) ?? 'Denied'

    /**
     * Worth showing at all — anything but `Denied`.
     *
     * **Includes `Partial`, so the server may still reject the call for specific arguments.** This is
     * the right default for visibility: a button that appears and then errors explains itself, while
     * one that silently never appears reads as a bug. For a DESTRUCTIVE action, reach for
     * [canFullyAccess] deliberately.
     */
    readonly canAccess = (route: RouteRef): boolean => this.getAccessLevel(route) !== 'Denied'

    /** Callable unconditionally, with no argument-dependent check. */
    readonly canFullyAccess = (route: RouteRef): boolean => this.getAccessLevel(route) === 'Granted'

    /**
     * Callable, but the server applies a further check on the ARGUMENTS — typically that an id in the
     * path is the caller's own, as with `/users/{id}/update`.
     *
     * The matrix cannot evaluate that; the caller must supply the equivalent rule:
     *
     * ```ts
     * const mayEdit = acl.canFullyAccess(api.updateUser)
     *     || (acl.canPartiallyAccess(api.updateUser) && user.id === session.userId)
     * ```
     */
    readonly canPartiallyAccess = (route: RouteRef): boolean =>
        this.getAccessLevel(route) === 'Partial'

    /** Never callable. */
    readonly isDenied = (route: RouteRef): boolean => this.getAccessLevel(route) === 'Denied'
}
