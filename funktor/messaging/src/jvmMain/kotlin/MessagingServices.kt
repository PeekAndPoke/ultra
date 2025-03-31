package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.messaging.storage.SentMessagesStorage

class MessagingServices(
    mailing: Lazy<Mailing>,
    sentMessages: Lazy<SentMessagesStorage>,
) {
    val mailing: Mailing by mailing
    val sentMessages: SentMessagesStorage by sentMessages
}
