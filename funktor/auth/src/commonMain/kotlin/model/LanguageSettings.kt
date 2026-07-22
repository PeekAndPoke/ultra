package io.peekandpoke.funktor.auth.model

import kotlinx.serialization.Serializable

/**
 * Per-user language / localisation settings.
 *
 * Deliberately a single extensible value object: new configuration values are added HERE (with
 * defaults), so the [AuthUser] interface and the auth/realm code never change when the settings
 * grow (timezone, display language, notification preferences are expected to join over time).
 */
@Serializable
data class LanguageSettings(
    /**
     * Language for messages sent to the user (emails, notifications), e.g. "de" or "de-CH".
     * `null` means "not set" — resolution falls back to the realm's default language.
     */
    val messaging: String? = null,
) {
    companion object {
        /** Default settings: nothing set, resolution falls back to the realm default. */
        val default = LanguageSettings()
    }
}
