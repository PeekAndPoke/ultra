package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Ref
import kotlinx.coroutines.runBlocking

object RefCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is String) {
            return null
        }

        val database = context.attributes[VaultSlumberModule.DatabaseKey]

        return if (database != null) {
            val cache = context.attributes[VaultSlumberModule.EntityCacheKey] ?: NullEntityCache

            val coll = data.split("/").first()

            cache.getOrPut(data) {
                // TODO: [AsyncIO] can we get rid of this runBlocking ?
                //       Probably the whole Slumber and all codecs would have to be async then ...
                runBlocking {
                    database.getRepository(coll).findById(data)
                }
            }?.asRef
        } else {
            null
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {

        is Ref<*> -> data._id

        else -> null
    }
}
