package de.peekandpoke.ktorfx.messaging

import de.peekandpoke.ktorfx.messaging.api.EmailAttachment
import de.peekandpoke.ktorfx.messaging.api.EmailBody
import de.peekandpoke.ktorfx.messaging.api.EmailDestination
import de.peekandpoke.ultra.common.TypedAttributes

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
)
