package io.peekandpoke.funktor.core.config.funktor

import io.peekandpoke.ultra.common.model.Redacted

import io.peekandpoke.funktor.core.config.funktor.cookies.CookieConsentConfig
import io.peekandpoke.funktor.core.config.funktor.tracking.TrackingConfig
import io.peekandpoke.funktor.core.model.InsightsConfig
import io.peekandpoke.ultra.security.UltraSecurityConfig
import io.peekandpoke.ultra.security.jwt.JwtConfig
import io.peekandpoke.ultra.vault.VaultConfig

/** Typed representation of the funktor configuration block (security, tracking, vault, auth, insights). */
data class FunktorConfig(
    val security: UltraSecurityConfig = UltraSecurityConfig(Redacted("CHANGE_ME"), 0L),
    val tracking: TrackingConfig = TrackingConfig(),
    val cookieConsent: CookieConsentConfig = CookieConsentConfig(),
    val vault: VaultConfig = VaultConfig(),
    val auth: AuthConfig = AuthConfig(),
    /**
     * The one insights switch for the whole app (ULTRA-00301): `funktor.insights.enabled`. Off by default; on outside
     * a development environment, the app refuses to start (funktor insights' `InsightsEnvironmentCheck`).
     */
    val insights: InsightsConfig = InsightsConfig(),
) {
    data class AuthConfig(
        val jwt: JwtConfig? = null,
        val baseUrls: Map<String, String> = emptyMap(),
    )
}
