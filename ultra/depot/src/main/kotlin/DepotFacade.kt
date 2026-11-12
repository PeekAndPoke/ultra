package io.peekandpoke.ultra.depot

import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotUri
import io.peekandpoke.ultra.depot.repos.DepotRepository
import kotlin.reflect.KClass

/**
 * The depot
 */
class DepotFacade(private val drivers: Lookup<DepotRepository<*>>) {

    init {
        // Two repositories on one place, or one inside the other, would read and write each other's files and
        // metadata: around the subject check, and around an encrypting repository
        val repos = drivers.all()

        repos.forEachIndexed { index, first ->
            repos.drop(index + 1).firstOrNull { first.overlaps(it) || it.overlaps(first) }?.let { second ->
                throw DepotException("The depot repositories '${first.name}' and '${second.name}' share their storage")
            }
        }
    }

    /**
     * Gets all repositories
     */
    fun getRepos() = drivers.all()

    /**
     * Gets a repository by it's [name]
     */
    fun getRepo(name: String): DepotRepository<*>? = drivers.all().firstOrNull { it.name == name }

    /**
     * Gets a repository by [cls]
     */
    fun <T : DepotRepository<*>> getRepo(cls: KClass<T>): T? = drivers.getOrNull(cls)

    /**
     * Gets the [DepotRepository] for the given [uri]
     */
    fun getRepo(uri: DepotUri): DepotRepository<*>? {
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
    suspend fun getContent(uri: DepotUri): DepotContent {
        return getRepo(uri)?.getContent(uri.path) ?: DepotContent.NotFound
    }

    /**
     * Get the meta-data for the given [uri]
     */
    suspend fun getMeta(uri: DepotUri): Map<String, String>? {
        return getRepo(uri)?.getMeta(uri.path)
    }
}
