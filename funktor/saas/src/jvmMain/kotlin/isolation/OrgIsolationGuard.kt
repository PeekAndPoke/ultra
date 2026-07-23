package io.peekandpoke.funktor.saas.isolation

import io.peekandpoke.funktor.rest.GuardVerdict
import io.peekandpoke.funktor.rest.RouteParamsGuard
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Request guard (a [RouteParamsGuard]) that enforces org-isolation for [OrgAwareParam] routes:
 *
 * 1. **Caller-binding** — the request's org must be the caller's SELECTED session org
 *    (`permissions.hasOrganisation(param.org._key)`; `isSuperUser` passes any org).
 * 2. **Org-consistency** — every resolved entity that IS an [OrgAware] must belong to that org
 *    (`entity.org hasSameIdAs param.org`). This is why the org logic lives in saas with CONCRETE
 *    types: `Ref<Organisation>.hasSameIdAs(Stored<Organisation>)` typechecks; a `Storable<*>` star
 *    projection would not.
 *
 * Org-ownership is decided by the entity's RUNTIME value (`value is OrgAware`), NOT the declared
 * `Stored<X>` type argument — so a polymorphic/base-typed param (`Stored<Base>` where `Base` is not
 * itself `OrgAware` but the concrete row is) is still checked. The cached field list is every
 * entity-ref (`Stored`/`Storable`-typed) property — the set the incoming converter loads.
 *
 * Any failure → [GuardVerdict.DenyAsNotFound] (404, hidden). Abstains on non-[OrgAwareParam] params.
 * No DB access: entity `org` refs and the loaded `param.org` expose `_id`/`_key` without resolving.
 *
 * NOTE on key spaces: caller-binding compares on `_key` (against `permissions.org`, a selected-org
 * key) while org-consistency compares on `_id` (the collection-qualified id `hasSameIdAs` uses). Both
 * sides of each comparison come from the same space — `param.org._key` vs the session key;
 * `entity.org._id` vs the loaded `param.org._id` — so the mix is correct, but see [OrgAware.org] for
 * the contract that an entity's `org` ref must carry the canonical `_id`.
 */
class OrgIsolationGuard : RouteParamsGuard {

    /** Per params-class cache of the entity-ref (`Stored`/`Storable`-typed) properties. */
    private val entityRefFields = ConcurrentHashMap<KClass<*>, List<KProperty1<Any, *>>>()

    override fun guard(params: Any, permissions: UserPermissions): GuardVerdict {
        val orgParam = params as? OrgAwareParam ?: return GuardVerdict.Pass

        // (1) caller-binding: the request org is the caller's selected org.
        if (!permissions.hasOrganisation(orgParam.org._key)) {
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

    // Every entity-ref property — the ones the incoming converter turns into a findById. Org-ownership
    // is decided per request by the runtime value, so this matches the LOADED set, not a static guess.
    @Suppress("UNCHECKED_CAST")
    private fun computeEntityRefFields(cls: KClass<*>): List<KProperty1<Any, *>> =
        cls.memberProperties
            .filter { it.returnType.classifier == Stored::class || it.returnType.classifier == Storable::class }
            .onEach { it.isAccessible = true }
            .map { it as KProperty1<Any, *> }
}
