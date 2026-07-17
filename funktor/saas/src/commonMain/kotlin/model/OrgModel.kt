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
