package de.peekandpoke.ktorfx.messaging.api

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SentMessageModel(
    val id: String,
    val refs: Set<String>,
    val tags: Set<String>,
    val content: Content,
    val attachments: List<EmailAttachment>,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
) {
    @Serializable
    sealed class Content {

        abstract fun getMessageId(): String?

        @Serializable
        @SerialName("email")
        data class EmailContent(
            val subject: String,
            val destination: EmailDestination,
            val source: String,
            val body: EmailBody,
            val result: EmailResult,
        ) : Content() {
            override fun getMessageId(): String? {
                return result.messageId
            }
        }
    }
}
