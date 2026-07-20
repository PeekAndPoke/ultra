package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.HasOrgMemberships
import io.peekandpoke.ultra.security.user.OrgMembership
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

/**
 * A b2b (customer-admin) tenant user. Carries [memberships] so the login flow can resolve the
 * user's accessible organisations and drive the 0/1/n org-selection (see [B2bRealm]).
 */
@Vault
data class B2bUser(
    val name: String,
    val email: String,
    override val memberships: Set<OrgMembership> = emptySet(),
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped, HasOrgMemberships {
    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
