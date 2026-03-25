package io.peekandpoke.funktor.cluster.storage.domain

import io.peekandpoke.ultra.datetime.MpInstant

data class TypedRandomData<T>(
    val category: String,
    val dataId: String,
    val data: T,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
)
