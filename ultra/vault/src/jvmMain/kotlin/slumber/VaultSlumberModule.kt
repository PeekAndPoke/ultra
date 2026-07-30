package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.EntityCache
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Stored
import kotlin.reflect.KType

/**
 * Slumber module that registers codecs for Vault domain types.
 *
 * Provides [Slumberer]s for [Ref], [Stored] and [New], but [Awaker]s only for [Ref] and [Stored]:
 * a [New] can be written to the database, never read back through this module. A declared [New]
 * type falls through to the built-in data-class awaker, which expects a nested `_value` instead of
 * the flat map [StoredSlumberer] writes — persisted documents come back as [Stored].
 *
 * Context attributes:
 * - [DatabaseKey] -- the [Database] used by [RefCodec] to resolve references.
 * - [EntityCacheKey] -- an optional [EntityCache] for deduplicating entity lookups during awakening.
 */
object VaultSlumberModule : SlumberModule {

    /** Attribute key for the [Database] instance used during deserialization. */
    val DatabaseKey = TypedKey<Database>("vault_Database")

    /** Attribute key for the [EntityCache] used to cache resolved entities during deserialization. */
    val EntityCacheKey = TypedKey<EntityCache>("vault_EntityCache")

    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {
        return when (type.classifier) {
            // A reference that cannot be read is an AwakerException naming the offending field,
            // rather than a null that quietly takes the whole document with it.
            // RefCodec is both an Awaker and a Slumberer, hence the cast to pick the overload.
            Ref::class -> type.wrapIfNonNull(RefCodec as Awaker)

            // NOT wrapped: a null row is how a missing document is reported. Karango's findById
            // issues DOCUMENT(repo, id), which yields null when nothing matches, and turns that
            // into a null result rather than an error.
            Stored::class -> type.arguments.firstOrNull()?.type?.let { StoredAwaker(it) }

            else -> null
        }
    }

    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {
        return when (type.classifier) {
            Ref::class -> RefCodec
            Stored::class -> StoredSlumberer
            New::class -> StoredSlumberer
            else -> null
        }
    }
}
