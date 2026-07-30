package io.peekandpoke.funktor.demo.server.admin

import io.peekandpoke.funktor.auth.model.AuthUser
import io.peekandpoke.funktor.auth.model.LanguageSettings
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.EmailAddress
import io.peekandpoke.ultra.vault.Vault
import io.peekandpoke.ultra.vault.hooks.Timestamped

@Vault
data class AdminUser(
    val name: String,
    override val email: EmailAddress,
    val isSuperUser: Boolean = false,
    override val language: LanguageSettings = LanguageSettings.default,
    override val createdAt: MpInstant = MpInstant.Epoch,
    override val updatedAt: MpInstant = createdAt,
) : Timestamped, AuthUser {
    override val displayName: String get() = name

    override fun withCreatedAt(instant: MpInstant) = copy(createdAt = instant)
    override fun withUpdatedAt(instant: MpInstant) = copy(updatedAt = instant)
}
