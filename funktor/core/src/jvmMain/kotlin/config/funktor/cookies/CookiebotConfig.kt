package de.peekandpoke.funktor.core.config.funktor.cookies

data class CookiebotConfig(
    val enabled: Boolean,
    val cbid: String = "",
    val blockingMode: String = "",
)
