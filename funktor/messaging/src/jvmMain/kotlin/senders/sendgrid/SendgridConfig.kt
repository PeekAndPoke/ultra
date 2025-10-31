package de.peekandpoke.funktor.messaging.senders.sendgrid

data class SendgridConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.eu.sendgrid.com",
)
