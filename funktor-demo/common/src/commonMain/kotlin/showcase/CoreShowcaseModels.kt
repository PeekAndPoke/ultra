package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class LifecycleHookInfo(
    val phase: String,
    val className: String,
    val executionOrder: Int,
)

@Serializable
data class ConfigInfoEntry(
    val key: String,
    val value: String,
)

@Serializable
data class CliCommandInfo(
    val name: String,
    val help: String,
)

@Serializable
data class FixtureInfo(
    val className: String,
    val dependsOn: List<String>,
)

@Serializable
data class RepairInfo(
    val className: String,
)

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
