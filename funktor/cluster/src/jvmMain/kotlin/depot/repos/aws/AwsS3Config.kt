package io.peekandpoke.funktor.cluster.depot.repos.aws

import io.peekandpoke.ultra.common.model.Redacted


data class AwsS3Config(
    val region: String,
    /** [Redacted] for parity with the name-based redaction this replaced — see `AwsSesConfig.accessKeyId`. */
    val accessKeyId: Redacted<String>,
    val secretAccessKey: Redacted<String>,
)
