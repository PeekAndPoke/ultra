package de.peekandpoke.ktorfx.messaging.senders.aws

import com.fasterxml.jackson.annotation.JsonIgnore

data class AwsSesConfig(
    val region: String,
    val accessKeyId: String,
    @get:JsonIgnore // Ignored so it will now show up in the logs or insights panel
    val secretAccessKey: String,
)
