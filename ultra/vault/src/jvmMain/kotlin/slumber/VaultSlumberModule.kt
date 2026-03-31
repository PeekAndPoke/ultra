package io.peekandpoke.ultra.vault.slumber

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.EntityCache
import io.peekandpoke.ultra.vault.LazyRef
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.slumber.VaultSlumberModule.DatabaseKey
import io.peekandpoke.ultra.vault.slumber.VaultSlumberModule.EntityCacheKey
import kotlin.reflect.KType

/**
 * Slumber module that registers codecs for Vault domain types.
 *
 * Provides [Awaker] and [Slumberer] implementations for [Ref], [LazyRef], [Stored], and [New].
 *
 * Context attributes:
 * - [DatabaseKey] -- the [Database] used by [RefCodec] and [LazyRefCodec] to resolve references.
 * - [EntityCacheKey] -- an optional [EntityCache] for deduplicating entity lookups during awakening.
 */
object VaultSlumberModule : SlumberModule {

    /** Attribute key for the [Database] instance used during deserialization. */
    val DatabaseKey = TypedKey<Database>("vault_Database")

    /** Attribute key for the [EntityCache] used to cache resolved entities during deserialization. */
    val EntityCacheKey = TypedKey<EntityCache>("vault_EntityCache")

    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {
        return when (type.classifier) {
            Ref::class -> RefCodec
            LazyRef::class -> LazyRefCodec
            Stored::class -> type.arguments.firstOrNull()?.type?.let { StoredAwaker(it) }
            else -> null
        }
    }

    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {
        return when (type.classifier) {
            Ref::class -> RefCodec
            LazyRef::class -> LazyRefCodec
            Stored::class -> StoredSlumberer
            New::class -> StoredSlumberer
            else -> null
        }
    }
}
