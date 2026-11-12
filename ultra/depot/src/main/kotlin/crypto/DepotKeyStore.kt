package io.peekandpoke.ultra.depot.crypto

import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The key of one data subject. Each use has a key of its own derived from it ([derive]): wrapping the keys of the
 * subject's encrypted files, signing its signed files. Erasing it makes the encrypted files unreadable and the
 * signed ones unverifiable; signed files stay readable.
 */
class DepotSubjectKey(val id: String, val subject: String, val key: ByteArray) {

    /** The key for one [purpose], derived from this one (HMAC-SHA-256, as HKDF expands): one key, one algorithm. */
    fun derive(purpose: String): ByteArray = Mac.getInstance("HmacSHA256")
        .apply { init(SecretKeySpec(key, "HmacSHA256")) }
        .doFinal(purpose.toByteArray(Charsets.UTF_8) + 1.toByte())

    companion object {
        /** Bytes of a subject's key: AES-256. */
        const val KEY_BYTES = 32

        private val random = SecureRandom()

        /** A new random key for [subject]. */
        fun create(subject: String): DepotSubjectKey =
            DepotSubjectKey(id = UUID.randomUUID().toString(), subject = subject, key = ByteArray(KEY_BYTES).also { random.nextBytes(it) })
    }
}

/** Where the subjects' keys live. A stored key is a credential: keep it out of logs, insights and API output. */
interface DepotKeyStore {
    /** The key of [subject] (a subject id's value), created when there is none. One subject has one key at a time. */
    suspend fun getOrCreate(subject: String): DepotSubjectKey

    /** The key with [id]; null when it was erased, or never existed. */
    suspend fun get(id: String): DepotSubjectKey?

    /**
     * Erases the key of [subject]; whether there was one. It throws when the key is there and could not be erased:
     * `false` only ever means "no key". Encrypted files written with it cannot be read again, except with a backup
     * of this store from before; signed files are not erased, only no longer verifiable.
     */
    suspend fun delete(subject: String): Boolean
}

/** A key store in memory, for tests: its keys are gone with the process. */
class InMemoryDepotKeyStore : DepotKeyStore {
    private val bySubject = ConcurrentHashMap<String, DepotSubjectKey>()

    /** How many subjects have a key. */
    val size: Int get() = bySubject.size

    override suspend fun getOrCreate(subject: String): DepotSubjectKey =
        bySubject.computeIfAbsent(subject) { DepotSubjectKey.create(it) }

    override suspend fun get(id: String): DepotSubjectKey? = bySubject.values.firstOrNull { it.id == id }

    override suspend fun delete(subject: String): Boolean = bySubject.remove(subject) != null
}
