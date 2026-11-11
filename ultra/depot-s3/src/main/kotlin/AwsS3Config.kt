package io.peekandpoke.ultra.depot.s3

import io.peekandpoke.ultra.common.model.Redacted


data class AwsS3Config(
    val region: String,
    /** [Redacted] for parity with the name-based redaction this replaced — see `AwsSesConfig.accessKeyId`. */
    val accessKeyId: Redacted<String>,
    val secretAccessKey: Redacted<String>,
)
