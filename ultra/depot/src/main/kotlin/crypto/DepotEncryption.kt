package io.peekandpoke.ultra.depot.crypto

import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Base64
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

/** An encrypted file: the bytes to store, and what the method needs besides the key to read them again. */
class DepotEncrypted(val bytes: ByteArray, val params: String)

/**
 * One way of encrypting a file. Its [name] is stored with every file it wrote (`depot-enc`), so files stay readable
 * after a deployment moves on to another method.
 */
interface DepotEncryption {
    /** Names the method, with all its choices: cipher, key wrapping, what is bound. Never reused for another. */
    val name: String

    /** Encrypts [plain] for the subject whose [key] this is. */
    fun encrypt(plain: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): DepotEncrypted

    /**
     * Decrypts [stored] with the [params] it was written with.
     *
     * @throws AEADBadTagException when the bytes, the params or the [context] are not what was written
     */
    fun decrypt(stored: ByteArray, params: String, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray

    companion object {
        /** Every method this version can read, by name. */
        val all: Map<String, DepotEncryption> = listOf<DepotEncryption>(Aes256GcmV1).associateBy { it.name }
    }
}

/**
 * AES-256-GCM over the whole file, with a random key per file. The file's key is wrapped (AES-256-GCM) by its
 * subject's key. Both are bound to the [DepotEncryptionContext]. The params are `iv.wrapIv.wrappedKey`, base64url.
 */
object Aes256GcmV1 : DepotEncryption {
    override val name = "aes256gcm-v1"

    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val PARTS = 3

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    override fun encrypt(plain: ByteArray, key: DepotSubjectKey, context: DepotEncryptionContext): DepotEncrypted {
        val aad = context.bytes()
        val fileKey = randomBytes(DepotSubjectKey.KEY_BYTES)
        val iv = randomBytes(IV_BYTES)
        val wrapIv = randomBytes(IV_BYTES)

        val bytes = cipher(Cipher.ENCRYPT_MODE, fileKey, iv, aad).doFinal(plain)
        val wrapped = cipher(Cipher.ENCRYPT_MODE, key.key, wrapIv, aad).doFinal(fileKey)

        return DepotEncrypted(bytes = bytes, params = listOf(iv, wrapIv, wrapped).joinToString(".") { encoder.encodeToString(it) })
    }

    override fun decrypt(stored: ByteArray, params: String, key: DepotSubjectKey, context: DepotEncryptionContext): ByteArray {
        val aad = context.bytes()

        val parts = try {
            params.split(".").map { decoder.decode(it) }
        } catch (_: IllegalArgumentException) {
            throw AEADBadTagException("Unreadable parameters")
        }

        if (parts.size != PARTS || parts[0].size != IV_BYTES || parts[1].size != IV_BYTES) {
            throw AEADBadTagException("Unreadable parameters")
        }

        val fileKey = cipher(Cipher.DECRYPT_MODE, key.key, parts[1], aad).doFinal(parts[2])

        if (fileKey.size != DepotSubjectKey.KEY_BYTES) throw AEADBadTagException("Unreadable key")

        return cipher(Cipher.DECRYPT_MODE, fileKey, parts[0], aad).doFinal(stored)
    }

    private fun randomBytes(size: Int) = ByteArray(size).also { random.nextBytes(it) }

    private fun cipher(mode: Int, key: ByteArray, iv: ByteArray, aad: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(aad)
        }
}
