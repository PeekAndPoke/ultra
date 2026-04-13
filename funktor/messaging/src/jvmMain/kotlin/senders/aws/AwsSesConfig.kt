package io.peekandpoke.funktor.messaging.senders.aws

import com.fasterxml.jackson.annotation.JsonIgnore

/** Configuration for the AWS SES email sender. */
data class AwsSesConfig(
    val region: String,
    val accessKeyId: String,
    @get:JsonIgnore // Ignored so it will now show up in the logs or insights panel
    val secretAccessKey: String,
)
