package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

// TODO: [TESTS] write some tests for me
@Serializable
data class AuditLog(
    val entries: List<Entry>
) {
    companion object {
        val empty = AuditLog(emptyList())
    }

    fun last(n: Int) = entries.takeLast(n).sortedByDescending { it.ts.toEpochMillis() }

    @Serializable
    data class Entry(
        val userId: String,
        val userType: String?,
        val userDesc: String?,
        val userEmail: String?,
        val clientIp: String?,
        val type: Type,
        val message: String,
        val ts: MpInstant,
    ) {
        @Suppress("EnumEntryName", "Detekt:EnumNaming")
        enum class Type {
            info,
            warning,
            error
        }
    }

    fun add(entry: Entry) = copy(entries = entries.plus(entry))
}
