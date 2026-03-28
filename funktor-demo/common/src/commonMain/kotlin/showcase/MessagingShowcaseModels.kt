package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class SendTestEmailRequest(
    val to: String,
    val subject: String = "Test Email from Funktor Demo",
    val body: String = "This is a test email sent from the funktor-demo showcase.",
    val isHtml: Boolean = false,
)

@Serializable
data class SendTestEmailResponse(
    val success: Boolean,
    val messageId: String?,
    val error: String?,
)

@Serializable
data class SentMessageInfo(
    val id: String,
    val to: String,
    val subject: String,
    val success: Boolean,
    val sentAt: String,
)

@Serializable
data class EmailSenderInfo(
    val type: String,
    val description: String,
)
