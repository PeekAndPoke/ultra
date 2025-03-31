package de.peekandpoke.funktor.core.websocket

import kotlinx.serialization.Serializable

@Serializable
data class WebsocketConnected(
    val sessionId: String,
)
