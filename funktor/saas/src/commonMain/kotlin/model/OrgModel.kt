package io.peekandpoke.funktor.saas.model

import kotlinx.serialization.Serializable

/**
 * Frontend-facing model of an [io.peekandpoke.funktor.saas.domain.Organisation].
 *
 * An organisation is the top-level tenant (e.g. a hotel chain); its [branches] are the sub-tenants
 * (e.g. the individual sites).
 */
@Serializable
data class OrgModel(
    /**
     * The organisation's bare Vault `_key`, NOT a collection-qualified `_id`.
     *
     * Deliberate, and the one place the project's "name a document by its full `_id`" rule does not
     * apply: this field exists to ADDRESS the org in a url (`/orgs/{id}` in the ops app, and
     * `OrgsApi.OrgParam`), where the collection comes from the route parameter's TYPE rather than from
     * the value — which is also why funktor's outgoing param converter renders every entity as its
     * `_key`. Contrast `ultra.security.user.OrgId` (`UserPermissions.org`, `OrgMembership.orgId`,
     * `AuthOrgRef.id`), which is always the full `_id`; project it with `OrgId.key` when you need a
     * url segment. Do NOT "standardize" this field to the `_id` — it would break every org url.
     */
    val id: String,
    val slug: String,
    val name: String,
    val status: OrgStatus,
    val branches: List<BranchModel> = emptyList(),
)

/** Frontend-facing model of a branch (sub-tenant) within an [OrgModel]. */
@Serializable
data class BranchModel(
    val id: String,
    val slug: String,
    val name: String,
    val status: OrgStatus,
)

/** Lifecycle status shared by organisations and branches. */
@Serializable
enum class OrgStatus {
    Active,
    Suspended,
    Archived,
}
