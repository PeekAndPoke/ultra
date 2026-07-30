package io.peekandpoke.funktor.saas.model

import kotlinx.serialization.Serializable

/**
 * An organisation's plan: the feature switches granted to everyone in the org (typically driven by
 * what the org has purchased).
 *
 * At login, the selected org's [featurePermissions] become the session's flat permissions (see
 * `buildOrgPermissions`), so per-org feature gating works via `permissions.hasPermission(...)`.
 */
@Serializable
data class OrgPlan(
    val name: String = "",
    val featurePermissions: Set<String> = emptySet(),
) {
    companion object {
        val none = OrgPlan()
    }
}
