package de.peekandpoke.ktorfx.messaging

import de.peekandpoke.ktorfx.messaging.storage.SentMessagesStorage

class MessagingServices(
    mailing: Lazy<Mailing>,
    sentMessages: Lazy<SentMessagesStorage>,
) {
    val mailing: Mailing by mailing
    val sentMessages: SentMessagesStorage by sentMessages
}
