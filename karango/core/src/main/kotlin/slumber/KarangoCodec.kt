package de.peekandpoke.karango.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule

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
