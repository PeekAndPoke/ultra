package de.peekandpoke.ktorfx.cluster.storage.domain

import de.peekandpoke.ktorfx.cluster.storage.RawCacheDataModel
import de.peekandpoke.ultra.common.datetime.MpInstant

data class TypedCacheData<T>(
    val category: String,
    val dataId: String,
    val data: T,
    val policy: RawCacheDataModel.Policy,
    val expiresAt: MpInstant,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
)
