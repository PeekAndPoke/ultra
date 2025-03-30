package de.peekandpoke.ktorfx.logging

import de.peekandpoke.ktorfx.logging.api.LogEntryModel
import de.peekandpoke.ultra.common.safeEnumOf
import de.peekandpoke.ultra.common.safeEnumsOf
import de.peekandpoke.ultra.logging.LogLevel
import kotlinx.serialization.Serializable

@Serializable
data class LogsFilter(
    val search: String = "",
    val minLevel: LogLevel = LogLevel.WARNING,
    val state: Set<LogEntryModel.State> = setOf(LogEntryModel.State.New),
    val page: Int = 1,
    val epp: Int = 100,
) {
    companion object {
        fun of(
            search: String,
            minLevel: String,
            state: String,
            page: Int,
            epp: Int,
        ) = LogsFilter(
            search = search,
            page = page,
            epp = epp,
            minLevel = safeEnumOf<LogLevel>(minLevel, LogLevel.WARNING),
            state = safeEnumsOf<LogEntryModel.State>(state).toSet(),
        )

        fun fromMap(map: Map<String, String>): LogsFilter {
            return of(
                minLevel = map["minLevel"] ?: "",
                state = map["state"] ?: "${LogEntryModel.State.New}",
                page = map["page"]?.toIntOrNull() ?: 1,
                epp = map["epp"]?.toIntOrNull() ?: 20,
                search = map["search"] ?: "",
            )
        }
    }

    fun firstPage() = copy(page = 1)

    fun toMap() = mapOf(
        "search" to search,
        "page" to page.toString(),
        "epp" to epp.toString(),
        "state" to state.joinToString(",") { it.name },
        "minLevel" to minLevel.name,
    )

    fun toRequestParams(): Array<Pair<String, String?>> {
        return toMap().entries.map { it.key to it.value }.toTypedArray()
    }
}
