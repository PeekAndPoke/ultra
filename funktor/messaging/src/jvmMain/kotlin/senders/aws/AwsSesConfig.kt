package io.peekandpoke.funktor.messaging.senders.aws

import io.peekandpoke.ultra.common.model.Redacted


/** Configuration for the AWS SES email sender. */
data class AwsSesConfig(
    val region: String,
    /**
     * [Redacted] for parity with the name-based redaction this replaced, which covered it.
     *
     * AWS documents an access key id as an identifier rather than a secret, so this is defence in
     * depth, not a credential leak: it is half of a key pair and it names the account. The principle
     * the deleted mechanism stated still applies — over-redacting a config value costs a debugging
     * detail, under-redacting one costs a credential.
     */
    val accessKeyId: Redacted<String>,
    val secretAccessKey: Redacted<String>,
)
