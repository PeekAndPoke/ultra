package io.peekandpoke.ultra.depot.crypto

import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** The key of one data subject: it wraps the keys of that subject's files. Erasing it erases the files for good. */
class DepotSubjectKey(val id: String, val subject: String, val key: ByteArray) {
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

    /** Erases every key of [subject]; whether there was one. Files written with it can never be read again. */
    suspend fun delete(subject: String): Boolean
}

/** A key store in memory, for tests: its keys are gone with the process. */
class InMemoryDepotKeyStore : DepotKeyStore {
    private val bySubject = ConcurrentHashMap<String, DepotSubjectKey>()

    override suspend fun getOrCreate(subject: String): DepotSubjectKey =
        bySubject.computeIfAbsent(subject) { DepotSubjectKey.create(it) }

    override suspend fun get(id: String): DepotSubjectKey? = bySubject.values.firstOrNull { it.id == id }

    override suspend fun delete(subject: String): Boolean = bySubject.remove(subject) != null
}
