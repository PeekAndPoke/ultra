package io.peekandpoke.funktor.insights

import java.time.LocalDateTime

data class InsightsData(
    val ts: LocalDateTime,
    val date: String,
    val startedNs: Long,
    val endedNs: Long,
    val collectors: List<CollectorData>,
)

/**
 * One collector's slice as stored on disk: its declared [key] plus the raw Jackson tree.
 *
 * [key] used to be the collector's JVM class name, which the loader fed to `Class.forName` — that ran
 * static initializers of whatever a stored file named, before any type check. A declared key is looked
 * up, never loaded.
 */
data class CollectorData(val key: String, val data: Map<*, *>)
