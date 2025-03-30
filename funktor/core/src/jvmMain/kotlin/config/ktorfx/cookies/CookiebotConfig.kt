package de.peekandpoke.ktorfx.core.config.ktorfx.cookies

data class CookiebotConfig(
    val enabled: Boolean,
    val cbid: String = "",
    val blockingMode: String = "",
)
