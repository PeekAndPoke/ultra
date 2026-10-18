package io.peekandpoke.funktor.insights

/**
 * One collector's slice of an insights record.
 *
 * Implementations are plain DTOs: they carry data and nothing else. The slice's key lives on
 * [InsightsCollector], not here — see the note there.
 *
 * VUE-REF: the original kotlinx.html rendering hooks are preserved at
 * `funktor/insights/reference/InsightsCollectorData.kt`.
 */
interface InsightsCollectorData
