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

/**
 * Mirrors `ApiAccessLevel` (`ultra/remote/src/commonMain/kotlin/ApiAccessLevel.kt`).
 *
 * **NOT named `ApiAccessLevel`, deliberately.** An SDK that reaches the auth feature generates that
 * exact name into `models.ts` — as both a const and a type — and the barrel `export *`s both files,
 * so sharing the name is a hard `TS2308` in every SDK this module exists to serve. Same reason
 * [AccessMatrix] and [AccessMatrixEntry] do not carry their Kotlin names.
 */
export type AccessLevel = 'Granted' | 'Partial' | 'Denied'

/** One row of the matrix. `uri` is the route pattern, placeholders included. */
export interface AccessMatrixEntry {
    readonly method: string
    readonly uri: string
    readonly level: AccessLevel
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
 * Both hold UNCONDITIONALLY, including for a level outside [AccessLevel] — a newer server, or a
 * matrix restored from an unvalidated cache. That is why only the two POSITIVE predicates test a
 * literal and [isDenied] is their negation: an unrecognised level then denies on all four rather
 * than making one of them the single predicate that fails open.
 *
 * Methods are arrow-function FIELDS, not prototype methods, so `const { canAccess } = acl` works.
 */
export class ApiAcl {
    private readonly lookup: Map<string, AccessLevel>

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
    readonly getAccessLevel = (route: RouteRef): AccessLevel =>
        this.lookup.get(`${route.method}|${route.uri}`) ?? 'Denied'

    /**
     * Worth showing at all — anything but `Denied`.
     *
     * **Includes `Partial`, and that is access.** `Partial` means the route is callable and the
     * server will additionally check the ARGUMENTS — typically "do you own this resource". The user
     * may use the route and its UI; enforcing the per-resource rule is the backend's job.
     *
     * So this is the predicate to reach for, including for DESTRUCTIVE actions. Gating a delete on
     * [canFullyAccess] would hide it from the user on their OWN resource, which is the silent
     * disappearance this default exists to avoid.
     *
     * A PUBLIC route short-circuits, because the matrix cannot answer for a logged-out visitor: the
     * endpoint serving it is itself authenticated, so without this an anonymous user is denied
     * `signIn` and can never reach a state where the matrix exists.
     */
    readonly canAccess = (route: RouteRef): boolean =>
        route.isPublic || this.canFullyAccess(route) || this.canPartiallyAccess(route)

    /**
     * The route's CALLER-LEVEL rule chain passes.
     *
     * **This is not a promise that the call will succeed.** A funktor handler may still reject it on
     * the arguments, and the matrix cannot see that: checks written inside a handler, and
     * `RouteParamsGuard`s such as the saas `OrgIsolationGuard`, are not `AuthRule`s and take no part
     * in `estimateAccess`. `AuthUserApi.setPassword` is the live example — its only rule is the
     * group's `authenticated()` floor, so it reports `Granted` to every logged-in user while the
     * handler enforces `userId == caller`.
     *
     * Narrow by design, and rarely what a view wants: use [canAccess] for "should this appear",
     * including for destructive actions. This answers the different question of whether the route
     * works regardless of WHICH resource it is pointed at — a bulk operation, an admin screen acting
     * across a set.
     *
     * **Deliberately does NOT short-circuit on `isPublic`**, unlike [canAccess]. For an anonymous
     * visitor with no matrix the honest answer is no, and widening it would leave the strict
     * predicate more permissive than the loose one.
     */
    readonly canFullyAccess = (route: RouteRef): boolean => this.getAccessLevel(route) === 'Granted'

    /**
     * Callable, but the server applies a further check on the ARGUMENTS — typically that an id in the
     * path is the caller's own, as with `/users/{id}/update`.
     *
     * **Reads false for every funktor route today.** No framework rule produces `Partial`: the DSL
     * leaves all yield `Granted` or `Denied`, and `and`/`or` fold with `maxOf`/`minOf`, so the level
     * cannot appear unless an app hand-writes `AccessLevelCheck { Partial }`. Kept because it mirrors
     * the Kotlin enum and because argument-scoped rules are expected to start reporting it — but do
     * NOT gate on it today expecting the branch to run.
     */
    readonly canPartiallyAccess = (route: RouteRef): boolean =>
        this.getAccessLevel(route) === 'Partial'

    /**
     * Never callable.
     *
     * The negation of [canAccess] rather than a test for `'Denied'`, so anything the two positive
     * predicates do not recognise lands here.
     */
    readonly isDenied = (route: RouteRef): boolean => !this.canAccess(route)
}
