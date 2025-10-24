package de.peekandpoke.karango.vault

import com.arangodb.ArangoCollection
import com.arangodb.ArangoCollectionAsync
import com.arangodb.model.CollectionCreateOptions
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.lang.IterableExpr

abstract class BaseRepository<T : Any>(
    override val name: String,
    override val storedType: TypeRef<T>,
    protected val driver: KarangoDriver,
) : Repository<T> {

    /**
     * The connection identifier of the repository
     */
    override val connection: String by lazy {
        val version = driver.getDatabaseVersion()

        "ArangoDB(${version.version})::${driver.arangoDb.name()}"
    }

    /**
     * Exposes the stored type as an expression
     */
    val repoExpr: IterableExpr<T> get() = IterableExpr("repo", this)

    /**
     * Returns the low-level [ArangoCollection]
     */
    fun getArangoCollection(): ArangoCollectionAsync {
        return driver.arangoDb.collection(name)
    }

    /**
     * Gives the chance to modify the options used for creating the collection
     */
    protected open fun modifyCollectionCreationOptions(options: CollectionCreateOptions): CollectionCreateOptions {
        return options
    }
}
