package io.peekandpoke.ultra.depot.repos

import io.peekandpoke.ultra.depot.domain.DepotFileContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId

/**
 * Definition of a depot repository.
 *
 * [T] is the kind of data subject its files belong to: a write takes a `DepotSubjectId<T>`, so a repository of
 * customer files cannot be written without saying which customer. A repository for anything is a
 * `DepotRepository<Any>`. Seen as `DepotRepository<*>` (looked up by name), it can be read, and written only for
 * [DepotSubjectId.system].
 */
interface DepotRepository<in T> {
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

    /**
     * The metadata of the file at [path]: what the writer gave, plus the depot's own entries such as
     * [DepotMetadata.SUBJECT]. Empty for a file stored without any, null when there is no such file.
     */
    suspend fun getMeta(path: String): Map<String, String>?

    /**
     * Puts a file with the given [path] and [content], for the data [subject] it is about. [meta] follows the rules
     * of [DepotMetadata]; the subject's id is stored with it.
     */
    suspend fun putFile(
        subject: DepotSubjectId<T>,
        path: String,
        content: ByteArray,
        meta: Map<String, String> = emptyMap(),
    ): DepotItem

    /** Puts a file with the given [path] and [content] */
    suspend fun putFile(
        subject: DepotSubjectId<T>,
        path: String,
        content: String,
        meta: Map<String, String> = emptyMap(),
    ): DepotItem {
        return putFile(subject = subject, path = path, content = content.toByteArray(), meta = meta)
    }

    /** Remove a file with a certain uri */
    suspend fun removeFile(path: String): Boolean
}
