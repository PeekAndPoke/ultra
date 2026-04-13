package io.peekandpoke.funktor.messaging.api

import kotlinx.serialization.Serializable

/** A base64-encoded email attachment with MIME type and filename. */
@Serializable
data class EmailAttachment(
    val mimeType: String,
    val filename: String,
    val dataBase64: String,
)
