@file:Suppress("PropertyName", "EnumEntryName")

package de.peekandpoke.funktor.messaging.senders.sendgrid

import de.peekandpoke.funktor.messaging.api.EmailAttachment
import kotlinx.serialization.Serializable

@Serializable
data class Email(val email: String, val name: String? = null)

@Serializable
data class Content(val type: String, val value: String)

@Serializable
data class Personalization(
    val to: List<Email>,
    val cc: List<Email>? = null,
    val bcc: List<Email>? = null,
    val subject: String? = null,
    val dynamic_template_data: Map<String, String>? = null,
)

@Serializable
data class Attachment(
    /** base64 of the bytes */
    val content: String,
    /** file name with attachment */
    val filename: String,
    /** MIME type, e.g. "application/pdf" */
    val type: String? = null,
    /** Disposition of the attachment */
    val disposition: Disposition? = Disposition.attachment,
    /** needed for inline (CID) only */
    val content_id: String? = null,
) {
    companion object {
        fun from(attachment: EmailAttachment) = Attachment(
            content = attachment.dataBase64,
            filename = attachment.filename,
            type = attachment.mimeType,
            disposition = Disposition.attachment,
        )
    }

    enum class Disposition { attachment, inline }
}

@Serializable
data class Mail(
    val from: Email,
    val personalizations: List<Personalization>,
    val subject: String? = null,
    val content: List<Content>? = null,
    val template_id: String? = null,
    val attachments: List<Attachment>? = null,
)
