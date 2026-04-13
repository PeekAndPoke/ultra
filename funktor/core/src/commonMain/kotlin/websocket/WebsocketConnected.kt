package io.peekandpoke.funktor.core.websocket

import kotlinx.serialization.Serializable

/** Sent to the client upon establishing a websocket connection. */
@Serializable
data class WebsocketConnected(
    val sessionId: String,
)
