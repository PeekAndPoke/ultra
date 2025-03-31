package de.peekandpoke.funktor.core.config.funktor.cookies

data class Ccm19Config(
    val enabled: Boolean,
    val apiKey: String?,
    val domain: String?,
) {
    fun getScriptUrl(): String? {
        if (apiKey == null || domain == null) {
            return null
        }

        return "https://cloud.ccm19.de/app.js?apiKey=${apiKey}&domain=${domain}"
    }
}
