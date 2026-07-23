package io.peekandpoke.funktor.demo.server.b2b2c

import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

/**
 * A b2b2c (end-user) tenant user — a B2B customer's own end-user. The login flow resolves the
 * user's accessible organisations from the saas `OrgMember` collection (see
 * [B2b2cRealm.getMemberships]) and drives the 0/1/n org-selection.
 */
@Vault
data class B2b2cUser(
    val name: String,
    override val email: String,
    override val language: LanguageSettings = LanguageSettings.default,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped, AuthUser {
    override val displayName: String get() = name

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
