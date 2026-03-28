package io.peekandpoke.funktor.demo.common.showcase

import kotlinx.serialization.Serializable

@Serializable
data class SseClockEvent(
    val timestamp: String,
    val epochMs: Long,
)

@Serializable
data class SseMetricsEvent(
    val heapUsedMb: Long,
    val heapMaxMb: Long,
    val threadCount: Int,
    val uptimeSeconds: Long,
)
