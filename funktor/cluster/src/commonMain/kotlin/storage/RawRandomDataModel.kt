package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.Serializable

@Serializable
data class RawRandomDataModel(
    val id: String,
    val category: String,
    val dataId: String,
    val data: String,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
) {
    @Serializable
    data class Head(
        val id: String,
        val category: String,
        val dataId: String,
        val size: Int,
        val createdAt: MpInstant,
        val updatedAt: MpInstant,
    )

    val asHead
        get() = Head(
            id = id,
            category = category,
            dataId = dataId,
            size = data.length,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
