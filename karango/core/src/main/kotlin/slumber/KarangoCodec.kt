package io.peekandpoke.karango.slumber

import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.EntityCache
import io.peekandpoke.ultra.vault.slumber.VaultSlumberModule

/**
 * Extends [Codec] with ArangoDB-specific context: [Database] for entity lookups and [EntityCache] for deduplication.
 *
 * Integrates [VaultSlumberModule] for serializing/deserializing entity references (Ref, LazyRef, Stored).
 */
class KarangoCodec(
    config: SlumberConfig,
    database: Database,
    val entityCache: EntityCache,
    configure: TypedAttributes.Builder.() -> Unit = {},
) : Codec(
    config = config.plusAttributes(
        TypedAttributes.of {
            add(VaultSlumberModule.DatabaseKey, database)
            add(VaultSlumberModule.EntityCacheKey, entityCache)

            configure()
        }
    ),
)
