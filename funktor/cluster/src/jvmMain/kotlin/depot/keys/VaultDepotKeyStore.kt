package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.depot.DepotSubjectErasedException
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.depot.crypto.DepotSubjectKey
import io.peekandpoke.ultra.vault.Repository
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * The depot's subject keys in the database: one record per subject, under a document key derived from the subject.
 * Erasing a subject empties its record and leaves it as a tombstone: the subject gets no key again.
 *
 * The keys are stored as they are: who reads the database reads the keys.
 */
class VaultDepotKeyStore(private val repo: Repo, private val kronos: Kronos) : DepotKeyStore {

    /** The repository of the key records. Its [insert] must fail when the document key is already taken. */
    interface Repo : Repository<DepotSubjectKeyRecord>

    override suspend fun getOrCreate(subject: String): DepotSubjectKey {
        val docKey = docKey(subject)

        repeat(CREATE_ATTEMPTS) { attempt ->
            repo.findById(docKey)?.value()?.let { found ->
                if (found.erasedAt != null) throw DepotSubjectErasedException()

                return found.asKey()
            }

            val created = DepotSubjectKey.create(subject)

            val record = DepotSubjectKeyRecord(
                // The document key in front: [get] finds the record by it
                keyId = "$docKey.${created.id}",
                subject = subject,
                key = Base64.getEncoder().encodeToString(created.key),
                createdAt = kronos.instantNow(),
            )

            try {
                return repo.insert(docKey, record).value().asKey()
            } catch (e: Throwable) {
                // Throwable: Karango's exceptions are no Exception. Never a cancellation or an Error.
                if (e is CancellationException || e is Error) throw e

                // Someone else is creating it at the same moment: theirs is the subject's key, once it is visible
                if (attempt == CREATE_ATTEMPTS - 1) throw e

                delay(CREATE_RETRY_MS * (attempt + 1))
            }
        }

        error("unreachable")
    }

    override suspend fun get(id: String): DepotSubjectKey? = live(id)?.takeIf { it.erasedAt == null }?.asKey()

    override suspend fun isErased(id: String): Boolean = live(id)?.erasedAt != null

    /** The record whose key has the [id]; null when the subject's record names another key, or there is none. */
    private suspend fun live(id: String): DepotSubjectKeyRecord? =
        repo.findById(id.substringBefore('.'))?.value()?.takeIf { it.keyId == id }

    /**
     * Whether there was a key. The record stays as a tombstone, without key and subject: it says the subject was
     * erased, by the hash of its id, and which key id that was.
     */
    override suspend fun delete(subject: String): Boolean {
        val docKey = docKey(subject)
        val now = kronos.instantNow()

        val stored = repo.findById(docKey)

        if (stored == null) {
            // Never had a key: erased all the same, so that no later write makes one
            repo.insert(docKey, DepotSubjectKeyRecord(keyId = "$docKey.none", subject = "", key = "", createdAt = now, erasedAt = now))

            return false
        }

        if (stored.value().erasedAt != null) return false

        repo.save(stored.modify { it.copy(subject = "", key = "", erasedAt = now) })

        return true
    }

    private fun DepotSubjectKeyRecord.asKey() = DepotSubjectKey(id = keyId, subject = subject, key = Base64.getDecoder().decode(key))

    /** A document key both databases take, the same for one subject: the SHA-256 of the subject, in hex. */
    private fun docKey(subject: String): String =
        MessageDigest.getInstance("SHA-256").digest(subject.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CREATE_ATTEMPTS = 5
        const val CREATE_RETRY_MS = 20L
    }
}
