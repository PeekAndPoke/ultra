package io.peekandpoke.funktor.messaging

import io.peekandpoke.funktor.messaging.api.EmailAttachment
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey

/**
 * Base interface for all email messages
 */
data class Email(
    /** The sender email address */
    val source: String,
    /** The destination addresses*/
    val destination: EmailDestination,
    /** The email subject */
    val subject: String,
    /** The email body */
    val body: EmailBody,
    /** The attachments to send with the email */
    val attachments: List<EmailAttachment> = emptyList(),
    /** Attributes */
    val attributes: TypedAttributes = TypedAttributes.empty,
) {
    companion object {
        val emailIdempotencyKey = TypedKey<String>("idempotencyKey")
    }

    fun withIdempotencyKey(key: String): Email = copy(attributes = attributes.plus(emailIdempotencyKey, key))

    fun getIdempotencyKey(): String? = attributes[emailIdempotencyKey]
}
