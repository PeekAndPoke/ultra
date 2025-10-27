package de.peekandpoke.monko

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.module
import de.peekandpoke.ultra.slumber.SlumberConfig
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.EntityCache
import de.peekandpoke.ultra.vault.slumber.VaultSlumberModule

fun KontainerBuilder.monko(config: MongoDbConfig) = module(MonkoModule, config)

fun getMonkoDefaultSlumberConfig() = SlumberConfig.default.prependModules(VaultSlumberModule)

/**
 * Monko kontainer module
 */
val MonkoModule = module { config: MongoDbConfig ->

    singleton(MongoClient::class) {
        config.toMongoClient()
    }

    singleton(MongoDatabase::class) { client: MongoClient ->
        client.getDatabase(config.database)
    }

    dynamic(MonkoDriver::class)

    // We initialize the SlumberConfig once, so we can re-use it.
    // This is important so for caching the codec lookups
    val codecConfig = getMonkoDefaultSlumberConfig()

    dynamic(MonkoCodec::class) { database: Database, cache: EntityCache ->
        MonkoCodec(
            config = codecConfig,
            database = database,
            entityCache = cache
        )
    }
}

private val dbCache = mutableMapOf<MongoDbConfig, MongoClient>()
private val lock = Any()

fun MongoDbConfig.toMongoClient(): MongoClient = synchronized(lock) {
    // TODO: check that the DB was not yet shut down
    dbCache[this]?.let { return it }

    dbCache.getOrPut(this) {
        toMongoClientWithoutCache()
    }
}

fun MongoDbConfig.toMongoClientWithoutCache(): MongoClient {
    val client = MongoClient.create(connectionString)

    return client
}
