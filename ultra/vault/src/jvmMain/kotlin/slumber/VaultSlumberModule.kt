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
import kotlin.reflect.KType

object VaultSlumberModule : SlumberModule {

    val DatabaseKey = TypedKey<Database>("vault_Database")
    val EntityCacheKey = TypedKey<EntityCache>("vault_EntityCache")

    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {
        return when (type.classifier) {
            Ref::class -> RefCodec
            LazyRef::class -> LazyRefCodec
            Stored::class -> StoredAwaker(type.arguments[0].type!!)
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
