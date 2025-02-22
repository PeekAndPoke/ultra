package de.peekandpoke.ultra.vault.slumber

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import de.peekandpoke.ultra.vault.LazyRef
import de.peekandpoke.ultra.vault.NullEntityCache
import de.peekandpoke.ultra.vault.Storable
import kotlinx.coroutines.runBlocking

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
                }!!
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
