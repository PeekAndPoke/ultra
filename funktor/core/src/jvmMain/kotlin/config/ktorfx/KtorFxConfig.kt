package de.peekandpoke.ktorfx.core.config.ktorfx

import de.peekandpoke.ktorfx.core.config.ktorfx.cookies.CookieConsentConfig
import de.peekandpoke.ktorfx.core.config.ktorfx.tracking.TrackingConfig
import de.peekandpoke.ultra.security.UltraSecurityConfig

data class KtorFxConfig(
    val security: UltraSecurityConfig = UltraSecurityConfig("CHANGE_ME", 0),
    val tracking: TrackingConfig = TrackingConfig(),
    val cookieConsent: CookieConsentConfig = CookieConsentConfig(),
)

