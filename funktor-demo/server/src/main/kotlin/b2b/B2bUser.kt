package io.peekandpoke.funktor.demo.server.b2b

import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

/**
 * A b2b (customer-admin) tenant user. The login flow resolves the user's accessible organisations
 * from the saas `OrgMember` collection (see [B2bRealm.getMemberships]) and drives the 0/1/n
 * org-selection.
 */
@Vault
data class B2bUser(
    val name: String,
    override val email: EmailAddress,
    override val language: LanguageSettings = LanguageSettings.default,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped, AuthUser {
    override val displayName: String get() = name

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
