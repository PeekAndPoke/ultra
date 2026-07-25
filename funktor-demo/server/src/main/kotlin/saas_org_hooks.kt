package io.peekandpoke.funktor.demo.server

import io.peekandpoke.funktor.auth.model.AuthOrgRef
import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.funktor.saas.storage.OrgsStorage
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.security.user.SelectedOrg
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.buildOrgPermissions

/*
 * Shared AuthRealm org-hook implementations over [OrgsStorage] for the OrgPolicy.Required realms
 * (b2b, b2b2c). Encodes the rules confirmed in the b2b review (20260720-b2b-realm.md):
 *
 * - ACTIVE ONLY: Suspended/Archived orgs are not sign-in-able, and since resolveSelectedOrg also
 *   guards select-org and token refresh, a mid-session suspension takes effect on the next refresh.
 * - Deduped by orgId: multiple membership rows for the same org list the org once in the picker.
 * - Deterministic on duplicates: roles/branches are merged (union), never picked by set order.
 */

/** The orgs a user may sign into (active only, deduped), for the login org-picker. */
suspend fun OrgsStorage.accessibleActiveOrgs(memberships: Set<OrgMembership>): List<AuthOrgRef> {
    return memberships
        .map { it.orgId }
        .distinct()
        .mapNotNull { orgId ->
            // `findById` takes the raw id; an OrgId is already the canonical `_id`.
            findById(orgId.value)
                ?.takeIf { it.value().status == OrgStatus.Active }
                ?.let { stored ->
                    val org = stored.value()
                    AuthOrgRef(id = OrgId(stored._id), slug = org.slug, name = org.name)
                }
        }
}

/** Resolves a chosen org into the session grant, or null (no membership / org not active). */
suspend fun OrgsStorage.resolveActiveSelectedOrg(
    orgId: OrgId,
    memberships: Set<OrgMembership>,
): SelectedOrg? {
    val matching = memberships.filter { it.orgId == orgId }
    if (matching.isEmpty()) return null

    val org = findById(orgId.value)
        ?.takeIf { it.value().status == OrgStatus.Active }
        ?.value()
        ?: return null

    val membership = OrgMembership(
        orgId = orgId,
        branchIds = matching.flatMap { it.branchIds }.toSet(),
        roles = matching.flatMap { it.roles }.toSet(),
    )

    return SelectedOrg(
        orgId = orgId,
        membership = membership,
        // DIVERGENCE SEAM: today every population selecting the org gets the full plan feature set.
        // When end-users (b2b2c) must receive a SUBSET of the org's features vs account-admins
        // (b2b), shape this grant by realm/role here — do not fork the whole hook per realm.
        planPermissions = org.plan.featurePermissions,
    )
}

/**
 * Builds the session [UserPermissions] from the VETTED membership set: the token's accessibleOrgs
 * claim must carry the same orgs the login picker shows (existing + active) — raw membership rows
 * may reference deleted or suspended orgs, which must not authorize `canAccessOrg()`.
 */
suspend fun OrgsStorage.buildVettedOrgPermissions(
    memberships: Set<OrgMembership>,
    selected: SelectedOrg?,
): UserPermissions {
    val accessibleIds = accessibleActiveOrgs(memberships).map { it.id }.toSet()

    return buildOrgPermissions(
        memberships = memberships.filter { it.orgId in accessibleIds }.toSet(),
        selected = selected,
    )
}
