package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class RetryDemoRequest(
    val maxAttempts: Int = 5,
    val failUntilAttempt: Int = 3,
    val delayMs: Long = 200,
)

@Serializable
data class RetryDemoResponse(
    val attempts: List<RetryAttemptLog>,
    val finalSuccess: Boolean,
)

@Serializable
data class RetryAttemptLog(
    val attempt: Int,
    val succeeded: Boolean,
    val message: String,
    val durationMs: Long,
)
