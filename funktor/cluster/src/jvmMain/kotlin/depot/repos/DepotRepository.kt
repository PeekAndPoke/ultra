package de.peekandpoke.ktorfx.cluster.depot.repos

import de.peekandpoke.ktorfx.cluster.depot.api.DepotItemModel.Meta
import de.peekandpoke.ktorfx.cluster.depot.domain.DepotFileContent
import de.peekandpoke.ktorfx.cluster.depot.domain.DepotItem
import depot.api.DepotRepositoryModel

/**
 * Definition of a depot repository
 */
interface DepotRepository {
    /** Unique name of the repository */
    val name: String

    /** Human-readable type of the repository */
    val type: String

    /** Human-readable storage location of the repository */
    val location: String

    /** Gets the parent path of the given path */
    fun getParentPath(path: String): String {
        return path.split('/')
            .filter { it.isNotBlank() }
            .dropLast(1)
            .joinToString("/")
    }

    /** Lists all files in the bucket */
    suspend fun listItems(path: String = ""): List<DepotItem>

    /** Lists [limit] newest files in the bucket */
    suspend fun listItems(path: String = "", limit: Int = 100): List<DepotItem>

    /** Gets the item with the given [path] */
    suspend fun getItem(path: String): DepotItem?

    /** Gets the file with the given [path] */
    suspend fun getFile(path: String): DepotItem.File?

    /** Gets the folder with the given [path] */
    suspend fun getFolder(path: String): DepotItem.Folder?

    /** Get the content of the file with the given [path] */
    suspend fun getContent(path: String): DepotFileContent?

    /** Get the meta information of the file with the given [path] */
    suspend fun getMeta(path: String): Meta?

    /** Puts a file with the given [path] and [content] */
    suspend fun putFile(
        path: String,
        content: ByteArray,
        options: DepotPutFileOptions.Builder.() -> Unit = {},
    ): DepotItem

    /** Puts a file with the given [path] and [content] */
    suspend fun putFile(
        path: String,
        content: String,
        options: DepotPutFileOptions.Builder.() -> Unit = {},
    ): DepotItem {
        return putFile(
            path = path,
            content = content.toByteArray(),
            options = options,
        )
    }

    /** Remove a file with a certain uri */
    suspend fun removeFile(path: String): Boolean

    /** Map to api model */
    fun asApiModel() = DepotRepositoryModel(
        name = name,
        type = type,
        location = location,
    )
}
