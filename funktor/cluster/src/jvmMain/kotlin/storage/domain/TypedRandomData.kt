package de.peekandpoke.ktorfx.cluster.storage.domain

import de.peekandpoke.ultra.common.datetime.MpInstant

data class TypedRandomData<T>(
    val category: String,
    val dataId: String,
    val data: T,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
)
