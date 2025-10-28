package de.peekandpoke.monko

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule

class MonkoCodec(
    config: SlumberConfig,
    database: Database,
    val entityCache: EntityCache,
    configure: TypedAttributes.Builder.() -> Unit = {},
) : Codec(
    config = config,
    attributes = TypedAttributes.of {
        add(VaultSlumberModule.DatabaseKey, database)
        add(VaultSlumberModule.EntityCacheKey, entityCache)

        configure()
    }
)
