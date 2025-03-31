package de.peekandpoke.funktor.core.config.funktor.tracking

data class GoogleTagManagerConfig(
    val container: String,
    val enabled: Boolean = true,
)
