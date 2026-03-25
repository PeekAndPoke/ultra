package io.peekandpoke.funktor.cluster.storage.domain

import io.peekandpoke.funktor.cluster.storage.RawCacheDataModel
import io.peekandpoke.ultra.datetime.MpInstant

data class TypedCacheData<T>(
    val category: String,
    val dataId: String,
    val data: T,
    val policy: RawCacheDataModel.Policy,
    val expiresAt: MpInstant,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
)
