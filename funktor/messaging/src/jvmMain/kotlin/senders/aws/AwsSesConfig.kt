package io.peekandpoke.funktor.messaging.senders.aws

import io.peekandpoke.ultra.common.model.Redacted


/** Configuration for the AWS SES email sender. */
data class AwsSesConfig(
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: Redacted<String>,
)
