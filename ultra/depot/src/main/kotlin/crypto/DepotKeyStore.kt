package io.peekandpoke.ultra.depot.crypto

import io.peekandpoke.ultra.depot.DepotSubjectErasedException
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
    /**
     * The key of [subject] (a subject id's value), created when there is none. One subject has one key, ever: an
     * erased subject's id is burnt, so subject ids must never be given to someone else. A key's id has at most
     * [MAX_KEY_ID_LENGTH] characters.
     *
     * @throws DepotSubjectErasedException when the subject was erased
     */
    suspend fun getOrCreate(subject: String): DepotSubjectKey

    /** The key with [id]; null when it was erased, or never existed: [isErased] tells which. */
    suspend fun get(id: String): DepotSubjectKey?

    /** Whether the key with [id] existed and was erased. False for an id no key ever had. */
    suspend fun isErased(id: String): Boolean

    /**
     * Erases the key of [subject] and remembers that the subject was erased: [getOrCreate] refuses it from now on.
     * Whether there was a key. It throws when the key could not be erased: `false` only ever means "no key".
     * Encrypted files written with it cannot be read again, except with a backup of this store from before; signed
     * files are not erased, only no longer verifiable.
     */
    suspend fun delete(subject: String): Boolean

    companion object {
        /** The longest id a key may have: what a write reserves in a file's metadata before the key is made. */
        const val MAX_KEY_ID_LENGTH = 128
    }
}

/** A key store in memory, for tests: its keys are gone with the process. */
class InMemoryDepotKeyStore : DepotKeyStore {
    private val bySubject = ConcurrentHashMap<String, DepotSubjectKey>()
    private val erasedSubjects: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val erasedIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** How many subjects have a key. */
    val size: Int get() = bySubject.size

    override suspend fun getOrCreate(subject: String): DepotSubjectKey = synchronized(this) {
        if (subject in erasedSubjects) throw DepotSubjectErasedException()

        bySubject.computeIfAbsent(subject) { DepotSubjectKey.create(it) }
    }

    override suspend fun get(id: String): DepotSubjectKey? = bySubject.values.firstOrNull { it.id == id }

    override suspend fun isErased(id: String): Boolean = id in erasedIds

    override suspend fun delete(subject: String): Boolean = synchronized(this) {
        erasedSubjects.add(subject)

        bySubject.remove(subject)?.also { erasedIds.add(it.id) } != null
    }
}
