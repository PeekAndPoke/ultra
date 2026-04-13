package io.peekandpoke.funktor.messaging

/** Configuration for development email behavior: disable sending, redirect, or ignore domains. */
data class MailingDevConfig(
    val disableEmails: Boolean = false,
    val destination: String? = null,
    val ignoreDomains: List<String> = emptyList(),
)
