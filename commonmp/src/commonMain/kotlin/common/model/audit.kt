package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.datetime.PortableDateTime
import kotlinx.serialization.Serializable

// TODO: [TESTS] write some tests for me
@Serializable
data class AuditLog(
    val entries: List<Entry>
) {
    companion object {
        val empty = AuditLog(emptyList())
    }

    @Serializable
    data class Entry(
        val userType: String,
        val userId: String,
        val type: Type,
        val message: String,
        val ts: PortableDateTime,
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
