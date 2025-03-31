package de.peekandpoke.funktor.core.config.funktor

import de.peekandpoke.funktor.core.config.funktor.cookies.CookieConsentConfig
import de.peekandpoke.funktor.core.config.funktor.tracking.TrackingConfig
import de.peekandpoke.ultra.security.UltraSecurityConfig

data class FunktorConfig(
    val security: UltraSecurityConfig = UltraSecurityConfig("CHANGE_ME", 0),
    val tracking: TrackingConfig = TrackingConfig(),
    val cookieConsent: CookieConsentConfig = CookieConsentConfig(),
)

