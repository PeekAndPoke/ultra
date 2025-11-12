package de.peekandpoke.funktor.core.config.funktor

import de.peekandpoke.funktor.core.config.funktor.cookies.CookieConsentConfig
import de.peekandpoke.funktor.core.config.funktor.tracking.TrackingConfig
import de.peekandpoke.ultra.security.UltraSecurityConfig
import de.peekandpoke.ultra.security.jwt.JwtConfig
import de.peekandpoke.ultra.vault.VaultConfig

data class FunktorConfig(
    val security: UltraSecurityConfig = UltraSecurityConfig("CHANGE_ME", 0),
    val tracking: TrackingConfig = TrackingConfig(),
    val cookieConsent: CookieConsentConfig = CookieConsentConfig(),
    val vault: VaultConfig = VaultConfig(),
    val auth: AuthConfig = AuthConfig(),
) {
    data class AuthConfig(
        val jwt: JwtConfig? = null,
        val baseUrls: Map<String, String> = emptyMap(),
    )
}
