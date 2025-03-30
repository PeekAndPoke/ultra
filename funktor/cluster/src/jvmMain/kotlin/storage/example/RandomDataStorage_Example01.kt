package de.peekandpoke.ktorfx.cluster.storage.example

import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorage

@Suppress("ClassName")
class RandomDataStorage_Example01(
    private val storage: RandomDataStorage,
) {
    private val key = RandomDataStorage.category<Data>("random-data-storage-example01")

    data class Data(
        val id: String,
        val text: String,
    )

    suspend fun store(data: Data) = storage.save(key, data.id, data)

    suspend fun load(id: String) = storage.load(key, id)
}
