package io.peekandpoke.funktor.cluster.depot.repos.aws

import io.peekandpoke.ultra.common.model.Redacted


data class AwsS3Config(
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: Redacted<String>,
)
