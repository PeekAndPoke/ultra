package io.peekandpoke.ultra.depot.crypto

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** What an encrypted file is bound to: decrypting under another path, subject, key or method fails. */
class DepotEncryptionContext(val method: String, val path: String, val subject: String, val keyId: String) {
    /** The four fields, each with its length before it, so no two contexts give the same bytes. */
    fun bytes(): ByteArray {
        val fields = listOf(method, path, subject, keyId).map { it.toByteArray(Charsets.UTF_8) }
        val buffer = ByteBuffer.allocate(fields.sumOf { Int.SIZE_BYTES + it.size })

        fields.forEach { buffer.putInt(it.size).put(it) }

        return buffer.array()
    }
}

/**
 * One way of encrypting a file. Its [name] is stored with every file it wrote (`depot-enc`), so files stay readable
 * after a deployment moves on to another method. What a method needs besides the subject's key to read a file
 * again, it keeps in the stored bytes themselves: content and its key material are never two things that can part.
 */
interface DepotEncryption {
    /** Names the method, with all its choices: cipher, key wrapping, layout, what is bound. Never reused for another. */
    val name: String

    /** Encrypts [plain] for the subject whose [key] this is; returns the bytes to store. */
    fun encrypt(plain: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray

    /**
     * Decrypts what [encrypt] returned.
     *
     * @throws GeneralSecurityException when the bytes or the [context] are not what was written
     */
    fun decrypt(stored: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray

    companion object {
        /** Every method this version can read, by name. */
        val all: Map<String, DepotEncryption> = listOf<DepotEncryption>(Aes256GcmV1).associateBy { it.name }
    }
}

/**
 * AES-256-GCM over the whole file, with a random key per file. The file's key is wrapped (AES-256-GCM) by a key
 * derived from its subject's key. Both are bound to the [DepotEncryptionContext]. Stored: the content's IV (12 bytes), the wrap's
 * IV (12), the wrapped key (48), the encrypted content.
 */
object Aes256GcmV1 : DepotEncryption {
    override val name = "aes256gcm-v1"

    private const val WRAP_PURPOSE = "depot aes256gcm-v1 wrap"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val WRAPPED_BYTES = DepotSubjectKey.KEY_BYTES + TAG_BITS / Byte.SIZE_BITS

    /** Bytes in front of the encrypted content. */
    private const val HEADER_BYTES = IV_BYTES + IV_BYTES + WRAPPED_BYTES

    private val random = SecureRandom()

    override fun encrypt(plain: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray = encrypt(
        plain = plain,
        key = key,
        context = context,
        fileKey = randomBytes(DepotSubjectKey.KEY_BYTES),
        iv = randomBytes(IV_BYTES),
        wrapIv = randomBytes(IV_BYTES),
    )

    /** [encrypt] with the random parts given: for the test that pins the stored format. */
    internal fun encrypt(
        plain: ByteArray,
        key: DepotSubjectKey,
        context: DepotEncryptionContext,
        fileKey: ByteArray,
        iv: ByteArray,
        wrapIv: ByteArray,
    ): ByteArray {
        val aad = context.bytes()

        val wrapped = cipher(Cipher.ENCRYPT_MODE, key.derive(WRAP_PURPOSE), wrapIv, aad).doFinal(fileKey)
        val content = cipher(Cipher.ENCRYPT_MODE, fileKey, iv, aad).doFinal(plain)

        return iv + wrapIv + wrapped + content
    }

    override fun decrypt(stored: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray {
        val iv = stored.part(0, IV_BYTES)
        val content = stored.copyOfRange(HEADER_BYTES, stored.size)

        return cipher(Cipher.DECRYPT_MODE, fileKey(stored, key, context), iv, context.bytes()).doFinal(content)
    }

    /** The file's own key, unwrapped with the subject's [key]. */
    internal fun fileKey(stored: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray {
        val wrapIv = stored.part(IV_BYTES, IV_BYTES)
        val wrapped = stored.part(IV_BYTES + IV_BYTES, WRAPPED_BYTES)

        return cipher(Cipher.DECRYPT_MODE, key.derive(WRAP_PURPOSE), wrapIv, context.bytes()).doFinal(wrapped)
    }

    private fun ByteArray.part(from: Int, size: Int): ByteArray {
        if (this.size < HEADER_BYTES) throw AEADBadTagException("Too short")

        return copyOfRange(from, from + size)
    }

    private fun randomBytes(size: Int) = ByteArray(size).also { random.nextBytes(it) }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(aad)
        }
}
