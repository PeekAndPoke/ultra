package io.peekandpoke.funktor.core.broker

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KParameter
import kotlin.reflect.KType

/**
 * OPT-IN marker for a route-params object whose entity references must be referentially consistent
 * with each other before the request is served. When present, the framework auto-appends a phase-2
 * auth rule that calls [isConsistent] on every request; a failure answers 404 (byte-identical to a
 * genuine not-found), never a distinguishable 403.
 *
 * Use it for NON-org referential consistency the framework can't derive — e.g. "this product belongs
 * to that category", nested graphs. There is **no default implementation** (a `= true` default would
 * be a silent hole); a dev opts in, and code-review / a future linter catch a route that should have.
 *
 * ## ConsistentParam is NOT a tenant boundary
 *
 * [isConsistent] checks the resolved entity graph against ITSELF — it never sees the caller. A
 * request for a FOREIGN org's entity, made through that foreign org's OWN url, is internally
 * consistent yet still cross-tenant. Cross-org ISOLATION is a separate, forced concern owned by the
 * saas `OrgAware` / `OrgAwareParam` interfaces + `OrgIsolationGuard` — implement those on org-scoped
 * routes; `ConsistentParam` is orthogonal (and optional).
 *
 * ```
 * data class ProductParams(
 *     override val org: Stored<Organisation>,  // OrgAwareParam → forced caller-binding + entity-org checks
 *     val product: Stored<Product>,            // Product : OrgAware → auto-checked against org
 *     val category: Stored<Category>,
 * ) : OrgAwareParam, ConsistentParam {         // ConsistentParam only for the non-org relation:
 *     override fun isConsistent(): Boolean = product.value.categoryId == category._key
 * }
 * ```
 */
interface ConsistentParam {
    /** Returns true when the params' entity references belong together. Keep it total and cheap. */
    fun isConsistent(): Boolean
}

/**
 * The ctor params of this type whose type is a resolved entity reference — `Stored<*>` or
 * `Storable<*>`. These are the params the [IncomingVaultConverter] turns into a `findById` DB read;
 * the shape a [io.peekandpoke.funktor.rest.RouteBootCheck] inspects to decide whether org-isolation
 * (or another cross-cutting check) applies.
 */
fun ReifiedKType.entityRefParams(): List<Pair<KParameter, KType>> =
    ctorParams2Types.filter { (_, type) ->
        type.classifier == Stored::class || type.classifier == Storable::class
    }
