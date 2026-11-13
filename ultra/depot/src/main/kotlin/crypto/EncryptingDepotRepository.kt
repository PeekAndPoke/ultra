package io.peekandpoke.ultra.depot.crypto

import io.peekandpoke.ultra.depot.DepotCorruptedException
import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.DepotSubjectErasedException
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRawWrite
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.depot.repos.DepotStorageId
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
 * - A file carries its own key material: content and metadata of one subject cannot part ways in a crash.
 * - Erased means: unreadable from every copy and backup of the files. A backup of the key store taken before
 *   the erasure still holds the key, until it expires.
 * - A listed size is the stored size, a few bytes more than the content.
 * - System files are refused: there is no subject whose key would protect them.
 */
abstract class EncryptingDepotRepository<in T>(
    private val inner: DepotRepository<T>,
    private val keys: DepotKeyStore,
    private val encryption: DepotEncryption = Aes256GcmV1,
    alsoReads: List<DepotEncryption> = emptyList(),
) : DepotRepository<T> {

    /** The methods this repository reads: the built-in ones, the one it writes with, and [alsoReads] (earlier ones). */
    private val readers: Map<String, DepotEncryption> =
        DepotEncryption.all + alsoReads.associateBy { it.name } + (encryption.name to encryption)

    override val name: String get() = inner.name

    override val type: String get() = "${inner.type}, encrypted"

    override val location: String get() = inner.location

    override val storageId: DepotStorageId get() = inner.storageId

    override suspend fun storedPath(path: String): String = inner.storedPath(path)

    override suspend fun listItems(path: String): List<DepotItem> = inner.listItems(path).map { it.wrapped() }

    override suspend fun listItems(path: String, limit: Int): List<DepotItem> = inner.listItems(path, limit).map { it.wrapped() }

    override suspend fun getItem(path: String): DepotItem? = inner.getItem(path)?.wrapped()

    override suspend fun getFile(path: String): DepotItem.File? = inner.getFile(path)?.let { MiddlewareFile(this, it) }

    override suspend fun getFolder(path: String): DepotItem.Folder? = inner.getFolder(path)?.let { MiddlewareFolder(this, it) }

    /** The metadata without the entries only the depot reads (the key's id). */
    override suspend fun getMeta(path: String): Map<String, String>? = inner.getMeta(path)?.minus(DepotMetadata.internal)

    /**
     * Compares the stored (encrypted) bytes with their hash and decrypts the file: only one that fits its hash and
     * decrypts is [DepotVerification.Intact]. Bytes replaced together with their hash, a file without its hash or
     * without its method are [DepotVerification.Damaged]. A shredded file is [DepotVerification.Unverifiable]:
     * its hash alone proves nothing.
     */
    override suspend fun verify(path: String): DepotVerification? {
        val meta = inner.getMeta(path) ?: return null

        // Every file of this repository names its method and has its hash
        if (DepotMetadata.ENC !in meta) return DepotVerification.Damaged

        val hashed = inner.verify(path) ?: return null

        return try {
            when (getContent(path)) {
                is DepotContent.Found -> if (hashed == DepotVerification.Intact) DepotVerification.Intact else DepotVerification.Damaged
                is DepotContent.Shredded -> DepotVerification.Unverifiable
                is DepotContent.NotFound -> null
            }
        } catch (_: DepotCorruptedException) {
            DepotVerification.Damaged
        }
    }

    override suspend fun removeFile(path: String): Boolean = inner.removeFile(path)

    /**
     * Reads and decrypts the file at [path]: [DepotContent.Shredded] when its subject's key was erased.
     *
     * @throws DepotCorruptedException when the file or its metadata is not what was written: it does not decrypt
     *   with its key, or it names no key, or one that never existed
     * @throws DepotException for a file that is not encrypted, or encrypted with a method this version cannot read
     */
    override suspend fun getContent(path: String): DepotContent {
        repeat(READ_ATTEMPTS) { attempt ->
            val meta = inner.getMeta(path) ?: return DepotContent.NotFound

            val method = meta[DepotMetadata.ENC] ?: throw DepotException("A file in '$name' is not encrypted")
            val reader = readers[method] ?: throw DepotException("A file in '$name' is encrypted with an unknown method")

            val subject = DepotMetadata.subjectOf(meta) ?: throw DepotCorruptedException()
            val keyId = meta[DepotMetadata.KEY] ?: throw DepotCorruptedException()

            // No key under that id: erased, or an id no key ever had (the entry was altered)
            val key = keys.get(keyId)
                ?: if (keys.isErased(keyId)) return DepotContent.Shredded else throw DepotCorruptedException()

            val stored = when (val read = inner.getContent(path)) {
                is DepotContent.Found -> read.bytes
                else -> return read
            }

            val context = DepotEncryptionContext(method = method, path = inner.storedPath(path), subject = subject, keyId = keyId)

            try {
                return DepotContent.Found(reader.decrypt(stored = stored, key = key, context = context))
            } catch (_: GeneralSecurityException) {
                // A write of this file for another subject may be under way: its metadata and its content are
                // replaced one after the other
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

        // The path, the metadata and its size with the depot's entries, before a key is made for the write. What
        // only the store can refuse (a folder at the path) comes after.
        val own = DepotMetadata.validate(meta)
        val storedPath = inner.storedPath(path)

        DepotMetadata.validateSize(
            DepotMetadata.stored(subject, own, content) +
                (DepotMetadata.ENC to encryption.name) +
                (DepotMetadata.KEY to "x".repeat(DepotKeyStore.MAX_KEY_ID_LENGTH))
        )

        val key = keys.getOrCreate(subject.value)

        val context = DepotEncryptionContext(
            method = encryption.name,
            path = storedPath,
            subject = subject.value,
            keyId = key.id,
        )

        val encrypted = encryption.encrypt(plain = content, key = key, context = context)

        val stored = DepotMetadata.stored(subject, own, encrypted) +
            (DepotMetadata.ENC to encryption.name) +
            (DepotMetadata.KEY to key.id)

        return inner.putRaw(path = path, content = encrypted, stored = stored).wrapped()
    }

    /** Not offered: a raw write would store bytes that are not encrypted. */
    @DepotRawWrite
    override suspend fun putRaw(path: String, content: ByteArray, stored: Map<String, String>): DepotItem =
        throw DepotException("An encrypting repository takes no raw writes")

    /**
     * Erases the [subject]'s key, in the key store: every file written for it, here and in every other repository
     * on that key store, reads as [DepotContent.Shredded] from now on (a signed one no longer verifies), and a
     * later write for the subject is refused with [DepotSubjectErasedException]. The files
     * and their metadata stay until they are removed. Whether there was a key; a key that could not be erased is an
     * exception, never `false`.
     */
    suspend fun shred(subject: DepotSubjectId<T>): Boolean {
        // No file here is a system file; the system's key signs the system files of signed repositories
        if (subject.value == DepotSubjectId.system.value) throw DepotException("The system is no data subject to erase")

        return keys.delete(subject.value)
    }

    private fun DepotItem.wrapped(): DepotItem = wrappedFor(this@EncryptingDepotRepository)

    private companion object {
        const val READ_ATTEMPTS = 3
        const val READ_RETRY_MS = 50L
    }
}
