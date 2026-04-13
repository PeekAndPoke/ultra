package io.peekandpoke.funktor.messaging.senders.sendgrid

/** Configuration for the SendGrid email sender. */
data class SendgridConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.eu.sendgrid.com",
)
