package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Storable
import io.peekandpoke.ultra.vault.VaultException

/**
 * Slumber codec for [Ref] instances.
 *
 * **Awaking:** expects a document-id string (e.g. `"collection/key"`) and returns a lazy [Ref].
 * The actual entity lookup is deferred to the [Ref.resolve] call, which runs in a suspend context.
 * No `runBlocking` — the caller pays for the resolution on their own coroutine.
 *
 * **Slumbering:** serializes a [Ref] back to its `_id` string.
 */
object RefCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is String) {
            return null
        }

        val database = context.attributes[VaultSlumberModule.DatabaseKey] ?: return null
        val cache = context.attributes[VaultSlumberModule.EntityCacheKey] ?: NullEntityCache
        val coll = data.split("/").first()

        // Lazy — the suspend resolver is stored, NOT executed during awake().
        // Resolution happens when the user calls ref.resolve() or ref() in a suspend context.
        @Suppress("UNCHECKED_CAST")
        return Ref.lazy<Any>(_id = data) {
            val found = cache.getOrPutAsync(data) {
                database.getRepository(coll).findById(data)
            }
            found as? Storable<Any> ?: throw VaultException("Referenced entity not found: $data")
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {
        is Ref<*> -> data._id
        else -> null
    }
}
