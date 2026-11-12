package io.peekandpoke.ultra.depot.crypto

import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRawWrite
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.depot.repos.DepotStorageId
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * A repository whose files are stored as they are, each with a keyed hash (HMAC-SHA-256) made with the key of its
 * data subject. [verify] then finds tampering, not only damage: who changes a file, its path, its subject or its
 * metadata cannot make a fitting hash without the key.
 *
 * It wraps the [inner] repository. Give it an inner repository nobody else can reach, as for
 * [EncryptingDepotRepository]. Reading does not check; [verify] is the call that does. The files are not secret,
 * and erasing a subject's key does not erase them: data that must be erasable belongs in an encrypting repository.
 */
abstract class SignedDepotRepository<in T>(
    private val inner: DepotRepository<T>,
    private val keys: DepotKeyStore,
) : DepotRepository<T> {

    override val name: String get() = inner.name

    override val type: String get() = "${inner.type}, signed"

    override val location: String get() = inner.location

    override val storageId: DepotStorageId get() = inner.storageId

    override suspend fun storedPath(path: String): String = inner.storedPath(path)

    override suspend fun listItems(path: String): List<DepotItem> = inner.listItems(path).map { it.wrappedFor(this) }

    override suspend fun listItems(path: String, limit: Int): List<DepotItem> = inner.listItems(path, limit).map { it.wrappedFor(this) }

    override suspend fun getItem(path: String): DepotItem? = inner.getItem(path)?.wrappedFor(this)

    override suspend fun getFile(path: String): DepotItem.File? = inner.getFile(path)?.let { MiddlewareFile(this, it) }

    override suspend fun getFolder(path: String): DepotItem.Folder? = inner.getFolder(path)?.let { MiddlewareFolder(this, it) }

    override suspend fun getContent(path: String): DepotContent = inner.getContent(path)

    /** The metadata without the entries only the depot reads (the key's id). */
    override suspend fun getMeta(path: String): Map<String, String>? = inner.getMeta(path)?.minus(DepotMetadata.internal)

    override suspend fun removeFile(path: String): Boolean = inner.removeFile(path)

    /** Stores [content] with a keyed hash over the content, the path, the subject and the [meta]. */
    @OptIn(DepotRawWrite::class)
    override suspend fun putFile(subject: DepotSubjectId<T>, path: String, content: ByteArray, meta: Map<String, String>): DepotItem {
        // Everything that can refuse the write, before a key is made for it
        val own = DepotMetadata.validate(meta)
        val storedPath = inner.storedPath(path)
        DepotMetadata.stored(subject, own, content)

        val key = keys.getOrCreate(subject.value)

        val mac = mac(key = key, path = storedPath, subject = subject.value, own = own, content = content)

        val stored = DepotMetadata.stored(subject, own, content) +
            (DepotMetadata.KEY to key.id) +
            (DepotMetadata.MAC to "$METHOD:$mac")

        return inner.putRaw(path = path, content = content, stored = stored).wrappedFor(this)
    }

    /** Not offered: a raw write would store a file without its keyed hash. */
    @DepotRawWrite
    override suspend fun putRaw(path: String, content: ByteArray, stored: Map<String, String>): DepotItem =
        throw DepotException("A signed repository takes no raw writes")

    /**
     * Compares the file at [path], its path, subject and metadata with the keyed hash stored at its write.
     * **Only [DepotVerification.Intact] proves a file.** A file without a keyed hash is
     * [DepotVerification.Damaged], and so is one naming a key that never existed.
     * [DepotVerification.Unverifiable] says the key that made the hash was erased. While the file is being written
     * it may answer [DepotVerification.Damaged] once.
     */
    override suspend fun verify(path: String): DepotVerification? {
        val meta = inner.getMeta(path) ?: return null
        // Every file of this repository has one: without it the file was not written here, or the entry was removed
        val expected = meta[DepotMetadata.MAC] ?: return DepotVerification.Damaged

        val subject = DepotMetadata.subjectOf(meta) ?: return DepotVerification.Damaged
        val keyId = meta[DepotMetadata.KEY] ?: return DepotVerification.Damaged
        // No key under that id: erased, or an id no key ever had (the entry was altered)
        val key = keys.get(keyId)
            ?: return if (keys.isErased(keyId)) DepotVerification.Unverifiable else DepotVerification.Damaged

        val content = when (val read = inner.getContent(path)) {
            is DepotContent.Found -> read.bytes
            is DepotContent.NotFound -> return null
            is DepotContent.Shredded -> throw DepotException("The content of a file in '$name' cannot be read")
        }

        val actual = "$METHOD:" + mac(
            key = key,
            path = inner.storedPath(path),
            subject = subject,
            own = DepotMetadata.withoutReserved(meta),
            content = content,
        )

        // In constant time: the comparison tells nothing about how much of a forged hash was right
        val same = MessageDigest.isEqual(actual.toByteArray(Charsets.UTF_8), expected.toByteArray(Charsets.UTF_8))

        return if (same) DepotVerification.Intact else DepotVerification.Damaged
    }

    /** The keyed hash, in hex: over every field with its length before it, the metadata in the order of its keys. */
    private fun mac(key: DepotSubjectKey, path: String, subject: String, own: Map<String, String>, content: ByteArray): String {
        val fields = listOf(METHOD, path, subject, key.id) + own.toSortedMap().flatMap { listOf(it.key, it.value) }

        val hmac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key.derive(MAC_PURPOSE), "HmacSHA256")) }

        hmac.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(fields.size).array())

        (fields.map { it.toByteArray(Charsets.UTF_8) } + listOf(content)).forEach { field ->
            hmac.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(field.size).array())
            hmac.update(field)
        }

        return hmac.doFinal().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        /** Names the hash with all its choices; stored in front of it. */
        const val METHOD = "hmac-sha256-v1"
        const val MAC_PURPOSE = "depot hmac-sha256-v1 sign"
    }
}
