package io.peekandpoke.funktor.core.config.funktor

import io.peekandpoke.funktor.core.config.funktor.cookies.CookieConsentConfig
import io.peekandpoke.funktor.core.config.funktor.tracking.TrackingConfig
import io.peekandpoke.ultra.security.UltraSecurityConfig
import io.peekandpoke.ultra.security.jwt.JwtConfig
import io.peekandpoke.ultra.vault.VaultConfig

/** Typed representation of the funktor configuration block (security, tracking, vault, auth). */
data class FunktorConfig(
    val security: UltraSecurityConfig = UltraSecurityConfig("CHANGE_ME", 0L),
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
