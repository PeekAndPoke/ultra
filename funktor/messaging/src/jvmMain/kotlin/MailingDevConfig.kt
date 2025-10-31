package de.peekandpoke.funktor.messaging

data class MailingDevConfig(
    val disableEmails: Boolean = false,
    val destination: String? = null,
    val ignoreDomains: List<String> = emptyList(),
)
