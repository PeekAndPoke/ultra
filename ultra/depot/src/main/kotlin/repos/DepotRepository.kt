package io.peekandpoke.ultra.depot.repos

import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification

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

    /** Reads the content of the file with the given [path]. */
    suspend fun getContent(path: String): DepotContent

    /**
     * The metadata of the file at [path]: what the writer gave, plus the depot's own entries such as
     * [DepotMetadata.SUBJECT] and [DepotMetadata.SHA256]. A file written before there was metadata, or not by the
     * depot, has no subject recorded: it reads without those entries, which is not the same as a system file. Null
     * when there is no such file.
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

    /**
     * For a middleware (the encrypting repository): writes [content] with the metadata [stored] exactly as given,
     * the depot's own entries included. Only the keys' form and the size are checked.
     */
    @DepotRawWrite
    suspend fun putRaw(path: String, content: ByteArray, stored: Map<String, String>): DepotItem

    /**
     * The path the file at [path] is stored under: the same for every way to reach one file (a link and its target
     * in the file system). What an encrypted file is bound to.
     */
    suspend fun storedPath(path: String): String = path

    /** The repository that holds the bytes: itself, or the one a middleware wraps. */
    val storage: DepotRepository<*> get() = this

    /** Whether this repository and [other] hold their files in the same place, or one inside the other. */
    fun overlaps(other: DepotRepository<*>): Boolean = storage.location == other.storage.location

    /** Puts a file with the given [path] and [content] */
    suspend fun putFile(
        subject: DepotSubjectId<T>,
        path: String,
        content: String,
        meta: Map<String, String> = emptyMap(),
    ): DepotItem {
        return putFile(subject = subject, path = path, content = content.toByteArray(), meta = meta)
    }

    /**
     * Reads the file at [path] and compares it with the hash stored at its write ([DepotMetadata.SHA256]). Null when
     * there is no such file. It finds damage: a truncated or altered file, content paired with other metadata. It
     * does not prove that nobody tampered: who can write the file and its metadata can write a fitting hash. While
     * the file is being written it may answer [DepotVerification.Damaged] once: verify again before acting on it.
     *
     * @throws DepotException when the file is there and its content cannot be read
     */
    suspend fun verify(path: String): DepotVerification? {
        val meta = getMeta(path) ?: return null
        val expected = meta[DepotMetadata.SHA256] ?: return DepotVerification.NoHash
        val content = when (val read = getContent(path)) {
            is DepotContent.Found -> read.bytes
            is DepotContent.NotFound -> return null
            is DepotContent.Shredded -> throw DepotException("The content of a file in '$name' cannot be read")
        }

        return if (DepotMetadata.sha256(content) == expected) DepotVerification.Intact else DepotVerification.Damaged
    }

    /** Remove a file with a certain uri */
    suspend fun removeFile(path: String): Boolean
}
