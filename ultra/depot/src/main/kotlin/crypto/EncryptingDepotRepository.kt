package io.peekandpoke.ultra.depot.crypto

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.depot.DepotCorruptedException
import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRawWrite
import io.peekandpoke.ultra.depot.repos.DepotRepository
import java.security.GeneralSecurityException
import kotlinx.coroutines.delay

/**
 * A repository whose files are encrypted, each with a key of its own that the key of its data subject wraps.
 * [shred] erases a subject's key, and with it every file of that subject, wherever a copy lies.
 *
 * It wraps the [inner] repository, which holds the encrypted bytes. Give it an inner repository nobody else can
 * reach: one that is no kontainer service of its own, or the files can be written and read around the encryption.
 *
 * - A file is bound to its path, subject, key and method: moved or relabelled, it does not decrypt.
 * - The metadata stays plain. [getMeta] leaves out what only the depot reads.
 * - A listed size is the stored size, a few bytes more than the content.
 * - System files are refused: there is no subject whose key would protect them.
 */
abstract class EncryptingDepotRepository<in T>(
    private val inner: DepotRepository<T>,
    private val keys: DepotKeyStore,
    private val encryption: DepotEncryption = Aes256GcmV1,
) : DepotRepository<T> {

    private class EncryptedFile(override val repo: EncryptingDepotRepository<*>, private val item: DepotItem.File) : DepotItem.File {
        override val path: String get() = item.path
        override val name: String get() = item.name
        override val lastModifiedAt: MpInstant? get() = item.lastModifiedAt
        override val size: Long? get() = item.size

        override suspend fun getContent(): DepotContent = repo.getContent(path)
    }

    private class EncryptedFolder(override val repo: EncryptingDepotRepository<*>, private val item: DepotItem.Folder) : DepotItem.Folder {
        override val path: String get() = item.path
        override val name: String get() = item.name
        override val lastModifiedAt: MpInstant? get() = item.lastModifiedAt
    }

    override val name: String get() = inner.name

    override val type: String get() = "${inner.type}, encrypted"

    override val location: String get() = inner.location

    override val storage: DepotRepository<*> get() = inner.storage

    override fun overlaps(other: DepotRepository<*>): Boolean = inner.overlaps(other)

    override suspend fun storedPath(path: String): String = inner.storedPath(path)

    override suspend fun listItems(path: String): List<DepotItem> = inner.listItems(path).map { it.wrapped() }

    override suspend fun listItems(path: String, limit: Int): List<DepotItem> = inner.listItems(path, limit).map { it.wrapped() }

    override suspend fun getItem(path: String): DepotItem? = inner.getItem(path)?.wrapped()

    override suspend fun getFile(path: String): DepotItem.File? = inner.getFile(path)?.let { EncryptedFile(this, it) }

    override suspend fun getFolder(path: String): DepotItem.Folder? = inner.getFolder(path)?.let { EncryptedFolder(this, it) }

    /** The metadata without the entries only the depot reads (the key's id, the method's parameters). */
    override suspend fun getMeta(path: String): Map<String, String>? = inner.getMeta(path)?.minus(DepotMetadata.internal)

    /** Compares the stored (encrypted) bytes with their hash: it needs no key, and works for a shredded file. */
    override suspend fun verify(path: String): DepotVerification? = inner.verify(path)

    override suspend fun removeFile(path: String): Boolean = inner.removeFile(path)

    /**
     * Reads and decrypts the file at [path]: [DepotContent.Shredded] when its subject's key was erased.
     *
     * @throws DepotCorruptedException when the key is there and the file does not decrypt
     * @throws DepotException for a file that is not encrypted, or encrypted with a method this version cannot read
     */
    override suspend fun getContent(path: String): DepotContent {
        repeat(READ_ATTEMPTS) { attempt ->
            val meta = inner.getMeta(path) ?: return DepotContent.NotFound

            val method = meta[DepotMetadata.ENC] ?: throw DepotException("A file in '$name' is not encrypted")
            val reader = DepotEncryption.all[method] ?: throw DepotException("A file in '$name' is encrypted with an unknown method")

            val subject = meta[DepotMetadata.SUBJECT] ?: throw DepotCorruptedException()
            val keyId = meta[DepotMetadata.KEY] ?: throw DepotCorruptedException()
            val params = meta[DepotMetadata.ENC_PARAMS] ?: throw DepotCorruptedException()

            val key = keys.get(keyId) ?: return DepotContent.Shredded

            val stored = when (val read = inner.getContent(path)) {
                is DepotContent.Found -> read.bytes
                else -> return read
            }

            val context = DepotEncryptionContext(method = method, path = inner.storedPath(path), subject = subject, keyId = keyId)

            try {
                return DepotContent.Found(reader.decrypt(stored = stored, params = params, key = key, context = context))
            } catch (_: GeneralSecurityException) {
                // A write of this file may be under way: its metadata and its content are replaced one after the other
                if (attempt == READ_ATTEMPTS - 1) throw DepotCorruptedException()

                delay(READ_RETRY_MS)
            }
        }

        throw DepotCorruptedException()
    }

    /** Encrypts [content] with a new key, wrapped by the [subject]'s key, and stores it. */
    @OptIn(DepotRawWrite::class)
    override suspend fun putFile(subject: DepotSubjectId<T>, path: String, content: ByteArray, meta: Map<String, String>): DepotItem {
        if (subject.value == DepotSubjectId.system.value) {
            throw DepotException("An encrypting repository takes no system files")
        }

        val own = DepotMetadata.validate(meta)
        val key = keys.getOrCreate(subject.value)

        val context = DepotEncryptionContext(
            method = encryption.name,
            path = inner.storedPath(path),
            subject = subject.value,
            keyId = key.id,
        )

        val encrypted = encryption.encrypt(plain = content, key = key, context = context)

        val stored = own +
            (DepotMetadata.SUBJECT to subject.value) +
            (DepotMetadata.SHA256 to DepotMetadata.sha256(encrypted.bytes)) +
            (DepotMetadata.ENC to encryption.name) +
            (DepotMetadata.KEY to key.id) +
            (DepotMetadata.ENC_PARAMS to encrypted.params)

        return inner.putRaw(path = path, content = encrypted.bytes, stored = stored).wrapped()
    }

    /** Not offered: a raw write would store bytes that are not encrypted. */
    @DepotRawWrite
    override suspend fun putRaw(path: String, content: ByteArray, stored: Map<String, String>): DepotItem =
        throw DepotException("An encrypting repository takes no raw writes")

    /**
     * Erases the [subject]'s key: every file written for it reads as [DepotContent.Shredded] from now on, for good.
     * The files and their metadata stay until they are removed. Whether there was a key.
     */
    suspend fun shred(subject: DepotSubjectId<T>): Boolean = keys.delete(subject.value)

    private fun DepotItem.wrapped(): DepotItem = when (this) {
        is DepotItem.File -> EncryptedFile(this@EncryptingDepotRepository, this)
        is DepotItem.Folder -> EncryptedFolder(this@EncryptingDepotRepository, this)
    }

    private companion object {
        const val READ_ATTEMPTS = 3
        const val READ_RETRY_MS = 50L
    }
}
