package io.peekandpoke.funktor.core.broker

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KParameter
import kotlin.reflect.KType

/**
 * Marks a route-params object whose entity references must be referentially consistent with each
 * other before the request is served.
 *
 * A route like `/orgs/{org}/entities/{entity}` resolves BOTH `org` and `entity` from the URL. The
 * entity id can be spoofed to point at a foreign org's entity while `{org}` still names the
 * caller's own org — a consistent-LOOKING request that is a cross-org IDOR. The param object must
 * assert the two belong together; forgetting the check is a silent hole.
 *
 * There is **deliberately no default implementation** — a `= true` default would recreate exactly
 * that silent hole. The framework FORCES a params object with two or more resolved entity
 * references (`Stored<*>` / `Storable<*>` ctor fields) to implement this at app-startup (see the
 * REST `ValidateRoutesOnAppStarting`), and auto-appends a phase-2 auth rule that calls
 * [isConsistent] on every request. A failure answers 404 (byte-identical to a genuine not-found),
 * never 403 — a distinguishable "exists, just not yours" would itself be a cross-org oracle.
 *
 * ## ConsistentParam is NOT a tenant boundary on its own
 *
 * [isConsistent] checks the resolved entity graph against ITSELF — it never sees the caller. A
 * request for a FOREIGN org's entity through that foreign org's OWN url is internally consistent yet
 * still cross-tenant. To bind the request to the caller you MUST also implement [OrgScopedParam].
 * The canonical org-scoped pattern implements BOTH:
 *
 * ```
 * data class MyParams(
 *     val org: Stored<Organisation>,
 *     val entity: Stored<Entity>,
 * ) : ConsistentParam, OrgScopedParam {
 *     override val orgId: String get() = org._key                      // caller must own this org
 *     override fun isConsistent(): Boolean = entity.value.orgId == org._key
 * }
 * ```
 */
interface ConsistentParam {
    /** Returns true when the params' entity references belong together. Keep it total and cheap. */
    fun isConsistent(): Boolean
}

/**
 * Marks a route-params object whose request is scoped to a single organisation, identified by
 * [orgId]. This is the actual TENANT BOUNDARY: the framework auto-appends a phase-2 auth rule that
 * binds the request to the caller's SELECTED session org — the caller's `permissions.org` must
 * equal [orgId] (`hasOrganisation`, `isSuperUser` excepted), otherwise the request answers 404
 * (byte-identical to not-found).
 *
 * It binds against the SELECTED org, not `accessibleOrgs` (the login picker's candidate set,
 * documented "Non-authz"): under the one-active-org-per-session model the session's permission slice
 * belongs to the selected org only, so a multi-org user who selected org A must NOT reach org B's
 * data without re-selecting.
 *
 * This is the second half of the cross-org IDOR defence, and the half that actually stops
 * cross-tenant access. [ConsistentParam] only checks the resolved entity graph against ITSELF — a
 * foreign org's entity, requested through that foreign org's OWN url, is internally "consistent" yet
 * still cross-tenant. Any org-scoped entity route needs THIS interface; [ConsistentParam] alone is
 * not enough. See [ConsistentParam] for the canonical both-interfaces example.
 */
interface OrgScopedParam {
    /** The organisation key the request addresses. Must equal the caller's SELECTED session org. */
    val orgId: String
}

/**
 * The ctor params of this type whose type is a resolved entity reference — `Stored<*>` or
 * `Storable<*>`. These are the params the [IncomingVaultConverter] turns into a `findById` DB read;
 * two or more of them in one route is the shape where cross-referential [ConsistentParam] checks
 * become necessary.
 */
fun ReifiedKType.entityRefParams(): List<Pair<KParameter, KType>> =
    ctorParams2Types.filter { (_, type) ->
        type.classifier == Stored::class || type.classifier == Storable::class
    }
