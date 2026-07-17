package io.peekandpoke.funktor.saas.domain

import io.peekandpoke.funktor.saas.model.OrgStatus
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

/**
 * An organisation is the top-level tenant (e.g. a hotel chain).
 *
 * Its [branches] are the sub-tenants (e.g. the individual sites of the chain), embedded directly
 * in the organisation document — a single organisation has a handful of branches, not thousands.
 */
@Vault
data class Organisation(
    val slug: String,
    val name: String,
    val status: OrgStatus = OrgStatus.Active,
    val branches: List<Branch> = emptyList(),
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped {

    /** A branch (sub-tenant) of an [Organisation]. */
    data class Branch(
        val id: String,
        val slug: String,
        val name: String,
        val status: OrgStatus = OrgStatus.Active,
    )

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
