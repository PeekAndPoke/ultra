package io.peekandpoke.ultra.security.user

/**
 * The organisation selected for the current session, plus the permissions granted by its plan.
 *
 * A `null` selection means an org-less session (admin / org-policy-none realms).
 */
data class SelectedOrg(
    val orgId: OrgId,
    val membership: OrgMembership,
    /** Feature permissions granted by the selected org's plan. */
    val planPermissions: Set<String> = emptySet(),
)

/**
 * Builds the [UserPermissions] for a session from the user's [memberships] and the [selected] org
 * (Model C — one active org per session):
 *
 * - `org` = the selected org id (null for org-less sessions)
 * - `accessibleOrgs` = every org the user may log into
 * - `branches` / `roles` = the selected org's slice (from the membership)
 * - `permissions` = the selected org's plan feature-permissions
 *
 * Apps layer additional grants on top via [UserPermissions.mergedWith].
 */
fun buildOrgPermissions(
    memberships: Set<OrgMembership>,
    selected: SelectedOrg?,
): UserPermissions = UserPermissions(
    org = selected?.orgId,
    accessibleOrgs = memberships.map { it.orgId }.toSet(),
    branches = selected?.membership?.branchIds ?: emptySet(),
    roles = selected?.membership?.roles ?: emptySet(),
    permissions = selected?.planPermissions ?: emptySet(),
)
