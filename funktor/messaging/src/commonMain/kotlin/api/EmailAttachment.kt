package de.peekandpoke.funktor.messaging.api

import kotlinx.serialization.Serializable

@Serializable
data class EmailAttachment(
    val mimeType: String,
    val filename: String,
    val dataBase64: String,
)
