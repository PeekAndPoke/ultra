package de.peekandpoke.funktor.insights

import java.time.LocalDateTime

data class InsightsData(
    val ts: LocalDateTime,
    val date: String,
    val startedNs: Long,
    val endedNs: Long,
    val collectors: List<CollectorData>,
)

data class CollectorData(val cls: String, val data: Map<*, *>)
