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
 * Awaking returns `null` — it never raises — both when the incoming data is not a String and when
 * [VaultSlumberModule.DatabaseKey] is absent from the context attributes. A `null` here propagates
 * outwards silently: the enclosing data class awakes to `null` as well (or falls back to a default
 * for an optional field), so a codec configured without the database attribute drops whole documents
 * instead of reporting the missing attribute.
 *
 * Failures of the deferred lookup surface only at resolve time: [Ref.resolve] throws [VaultException]
 * when the collection has no repository or the document does not exist. [VaultException] extends
 * `Throwable`, not `Exception`, so a surrounding `catch (e: Exception)` does NOT catch it.
 *
 * **Slumbering:** serializes a [Ref] back to its `_id` string; any other input slumbers to `null`.
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
        return Ref.lazy(_id = data) {
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
