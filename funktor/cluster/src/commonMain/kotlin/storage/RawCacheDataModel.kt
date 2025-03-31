package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.ultra.common.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
data class RawCacheDataModel(
    val id: String,
    val category: String,
    val dataId: String,
    val data: String,
    val policy: Policy,
    val expiresAt: MpInstant,
    val createdAt: MpInstant,
    val updatedAt: MpInstant,
) {
    @Serializable
    data class Head(
        val id: String,
        val category: String,
        val dataId: String,
        val size: Int,
        val policy: Policy,
        val expiresAt: MpInstant,
        val createdAt: MpInstant,
        val updatedAt: MpInstant,
    )

    @Serializable
    sealed class Policy {

        /** Time to live in seconds */
        abstract val ttl: Long

        @Serializable
        @SerialName("serve-cache-and-refresh")
        data class ServeCacheAndRefresh(
            /** Time to live in seconds */
            override val ttl: Long,
            /** Time in seconds after which the cached value should be refreshed */
            val refreshAfter: Long = ttl / 2,
        ) : Policy() {

            companion object {
                operator fun invoke(ttl: Duration, refreshAfter: Duration = ttl / 2): ServeCacheAndRefresh {
                    return ServeCacheAndRefresh(
                        ttl = ttl.inWholeSeconds,
                        refreshAfter = refreshAfter.inWholeSeconds,
                    )
                }
            }

        }
    }

    val asHead
        get() = Head(
            id = id,
            category = category,
            dataId = dataId,
            size = data.length,
            policy = policy,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
