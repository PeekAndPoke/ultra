package io.peekandpoke.funktor.saas.isolation

import io.peekandpoke.funktor.rest.GuardVerdict
import io.peekandpoke.funktor.rest.RouteParamsGuard
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Request guard (a [RouteParamsGuard]) that enforces org-isolation for [OrgAwareParam] routes:
 *
 * 1. **Caller-binding** — the request's org must be the caller's SELECTED session org
 *    (`permissions.hasOrganisation(param.org._id as OrgId)`; `isSuperUser` passes any org).
 * 2. **Org-consistency** — every resolved entity that IS an [OrgAware] must belong to that org
 *    (`entity.org hasSameIdAs param.org`). This is why the org logic lives in saas with CONCRETE
 *    types: `Ref<Organisation>.hasSameIdAs(Stored<Organisation>)` typechecks; a `Storable<*>` star
 *    projection would not.
 *
 * Org-ownership is decided by the entity's RUNTIME value (`value is OrgAware`), NOT the declared
 * `Stored<X>` type argument — so a polymorphic/base-typed param (`Stored<Base>` where `Base` is not
 * itself `OrgAware` but the concrete row is) is still checked. The cached field list is every
 * `Stored`/`Storable`-typed STORED property (with a backing field) — a SUPERSET of the converter's
 * loaded set, so no readable loaded entity escapes; over-checking only ever fails closed.
 *
 * Any failure → [GuardVerdict.DenyAsNotFound] (404, hidden). Abstains on non-[OrgAwareParam] params.
 * No DB access: entity `org` refs and the loaded `param.org` expose `_id`/`_key` without resolving.
 *
 * Key spaces: BOTH checks now compare collection-qualified `_id`s — caller-binding against
 * `permissions.org` (an [OrgId], which the type guarantees is an `_id`) and org-consistency via
 * `hasSameIdAs`. There is no `_key`-vs-`_id` mix left to get wrong; a bare `_key` reaching either side
 * cannot even be constructed. See [OrgAware.org] for the matching entity-side contract.
 */
class OrgIsolationGuard : RouteParamsGuard {

    /** Per params-class cache of the entity-ref (`Stored`/`Storable`-typed) properties. */
    private val entityRefFields = ConcurrentHashMap<KClass<*>, List<KProperty1<Any, *>>>()

    override fun guard(params: Any, permissions: UserPermissions): GuardVerdict {
        val orgParam = params as? OrgAwareParam ?: return GuardVerdict.Pass

        // (1) caller-binding: the request org is the caller's selected org.
        //
        // `parseOrNull`, NOT the raw `OrgId(...)` ctor. This is the tenant boundary and its contract
        // (below) is that ANY failure denies silently, so a `Stored<Organisation>` whose `_id` is not
        // a well-formed `collection/key` — a hand-built fixture, a non-vault `OrgAwareParam` impl, a
        // corrupted row — must deny like any other mismatch. Letting the ctor throw here would break
        // that contract twice over: a 500 instead of a 404, and a probe that distinguishes "malformed
        // org" from "not your org".
        //
        // The raw ctor would double as a tripwire for a regression to `._key` (loud 500 rather than a
        // silent deny), but that tripwire belongs in CI, not in production: reverting this line to
        // `._key` is caught by 7 tests in `OrgIsolationSpec`. A log line here would be the ideal
        // middle ground, but this guard caches [entityRefFields] per params-class and must stay a real
        // singleton — a `Log` ctor dependency would make it per-kontainer and throw the cache away.
        val requestedOrg = OrgId.parseOrNull(orgParam.org._id)
            ?: return GuardVerdict.DenyAsNotFound

        if (!permissions.hasOrganisation(requestedOrg)) {
            return GuardVerdict.DenyAsNotFound
        }

        // (2) org-consistency: every entity whose RUNTIME value is OrgAware belongs to that org.
        val fields = entityRefFields.getOrPut(params::class) { computeEntityRefFields(params::class) }

        for (field in fields) {
            val entity = (field.get(params) as? Stored<*>)?.value
            if (entity is OrgAware && !entity.org.hasSameIdAs(orgParam.org)) {
                return GuardVerdict.DenyAsNotFound
            }
        }

        return GuardVerdict.Pass
    }

    // Every `Stored`/`Storable`-typed STORED property (one with a backing field): the url-loaded ctor
    // params AND any property that re-holds a loaded entity (e.g. an aliased non-`val` ctor param).
    // This is a SUPERSET of the incoming converter's loaded set, so no handler-readable loaded
    // OrgAware entity can escape the org check — over-checking only ever DENIES (fail-closed), never
    // an IDOR. The backing-field filter drops computed/derived `Stored` getters, which are never a
    // url-loaded entity and must not be executed here (keeping the guard's "no DB access" invariant).
    @Suppress("UNCHECKED_CAST")
    private fun computeEntityRefFields(cls: KClass<*>): List<KProperty1<Any, *>> =
        cls.memberProperties
            .filter { it.returnType.classifier == Stored::class || it.returnType.classifier == Storable::class }
            .filter { it.javaField != null }
            .onEach { it.isAccessible = true }
            .map { it as KProperty1<Any, *> }
}
