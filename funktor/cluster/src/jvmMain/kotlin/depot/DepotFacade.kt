package io.peekandpoke.funktor.cluster.depot

import io.peekandpoke.funktor.cluster.depot.domain.DepotFileContent
import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.cluster.depot.domain.DepotUri
import io.peekandpoke.funktor.cluster.depot.repos.DepotRepository
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotItemModel
import io.peekandpoke.ultra.common.Lookup
import kotlin.reflect.KClass

/**
 * The depot
 */
class DepotFacade(private val drivers: Lookup<DepotRepository>) {

    /**
     * Gets all repositories
     */
    fun getRepos() = drivers.all()

    /**
     * Gets a repository by it's [name]
     */
    fun getRepo(name: String): DepotRepository? = drivers.all().firstOrNull { it.name == name }

    /**
     * Gets a repository by [cls]
     */
    fun <T : DepotRepository> getRepo(cls: KClass<T>): T? = drivers.getOrNull(cls)

    /**
     * Gets the [DepotRepository] for the given [uri]
     */
    fun getRepo(uri: DepotUri): DepotRepository? {
        return getRepo(uri.repo)
    }

    /**
     * Gets the [DepotItem] for the given [uri]
     */
    suspend fun getFile(uri: DepotUri): DepotItem.File? {
        return getRepo(uri)?.getFile(uri.path)
    }

    /**
     * Gets the [DepotItem] for the given [uri]
     */
    suspend fun removeFile(uri: DepotUri): Boolean? {
        return getRepo(uri)?.removeFile(uri.path)
    }

    /**
     * Get the contents for the given [uri]
     */
    suspend fun getContent(uri: DepotUri): DepotFileContent? {
        return getRepo(uri)?.getContent(uri.path)
    }

    /**
     * Get the meta-data for the given [uri]
     */
    suspend fun getMeta(uri: DepotUri): DepotItemModel.Meta? {
        return getRepo(uri)?.getMeta(uri.path)
    }
}
