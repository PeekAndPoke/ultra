package de.peekandpoke.funktor.cluster.storage.domain

import de.peekandpoke.funktor.cluster.storage.RawCacheDataModel
import de.peekandpoke.karango.Karango
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.vault.hooks.Timestamped

@Karango
data class RawCacheData(
    /** Storage category key */
    val category: String,
    /** Data id within the category */
    val dataId: String,
    /** The data to be held */
    val data: Any?,
    /** The retention / refresh policy */
    val policy: RawCacheDataModel.Policy,
    /** Expiration timestamp in Epoch Seconds */
    val expiresAt: Long,
    /** Instant of creation */
    override val createdAt: MpInstant = MpInstant.Epoch,
    /** Instant of last update */
    override val updatedAt: MpInstant = MpInstant.Epoch,
) : Timestamped {

    override fun withCreatedAt(instant: MpInstant): Timestamped = copy(createdAt = instant)

    override fun withUpdatedAt(instant: MpInstant): Timestamped = copy(updatedAt = instant)

    fun <T> asTyped(mapper: (RawCacheData) -> T) = TypedCacheData(
        category = category,
        dataId = dataId,
        data = mapper(this),
        policy = policy,
        expiresAt = MpInstant.fromEpochSeconds(expiresAt),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
