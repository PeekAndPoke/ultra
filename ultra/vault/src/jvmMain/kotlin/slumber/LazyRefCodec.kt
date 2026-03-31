package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.LazyRef
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.VaultException
import kotlinx.coroutines.runBlocking

/**
 * Slumber codec for [LazyRef] instances.
 *
 * **Awaking:** expects a document-id string (e.g. `"collection/key"`) and creates a [LazyRef]
 * whose value is resolved lazily from the database on first access, using an [EntityCache]
 * when available.
 *
 * **Slumbering:** serializes a [LazyRef] back to its `_id` string.
 */
object LazyRefCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is String) {
            return null
        }

        val database = context.attributes[VaultSlumberModule.DatabaseKey]

        return if (database != null) {
            val cache = context.attributes[VaultSlumberModule.EntityCacheKey] ?: NullEntityCache

            val coll = data.split("/").first()

            LazyRef(_id = data) { id ->
                cache.getOrPut(id) {
                    // TODO: [AsyncIO] can we get rid of this runBlocking ?
                    //       Probably the whole Slumber and all codecs would have to be async then ...
                    runBlocking {
                        database.getRepository(coll).findById(id) as Storable<*>
                    }
                } ?: throw VaultException("Referenced entity not found: $id")
            }
        } else {
            null
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {

        is LazyRef<*> -> data._id

        else -> null
    }
}
