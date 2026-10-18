package io.peekandpoke.funktor.messaging.senders.sendgrid

import io.peekandpoke.ultra.common.model.Redacted

/** Configuration for the SendGrid email sender. */
data class SendgridConfig(
    /**
     * A full-power SendGrid credential — sent as `Authorization: Bearer`.
     *
     * [Redacted] because the whole `AppConfig` tree is slumbered into every FULL insights record. It was
     * a plain `String` between 2026-07-31's `ConfigRedaction` deletion and the review that caught it:
     * the name-based redaction that used to cover it matched `apiKey`, and converting only the fields
     * that had `@JsonIgnore` was a net LOSS of coverage rather than a like-for-like replacement.
     */
    val apiKey: Redacted<String>,
    val baseUrl: String = "https://api.eu.sendgrid.com",
)
