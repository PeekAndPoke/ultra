package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.depot.crypto.DepotSubjectKey
import io.peekandpoke.ultra.vault.Repository
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * The depot's subject keys in the database: one record per subject, under a document key derived from the subject.
 * Erasing a subject removes its record; a file names its key by an id no later key of that subject has again.
 *
 * The keys are stored as they are: who reads the database reads the keys.
 */
class VaultDepotKeyStore(private val repo: Repo, private val kronos: Kronos) : DepotKeyStore {

    /** The repository of the key records. Its [insert] must fail when the document key is already taken. */
    interface Repo : Repository<DepotSubjectKeyRecord>

    override suspend fun getOrCreate(subject: String): DepotSubjectKey {
        val docKey = docKey(subject)

        repeat(CREATE_ATTEMPTS) { attempt ->
            repo.findById(docKey)?.let { return it.value().asKey() }

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

    override suspend fun get(id: String): DepotSubjectKey? {
        val record = repo.findById(id.substringBefore('.'))?.value() ?: return null

        // The subject's current key is another one: the key asked for was erased
        return record.takeIf { it.keyId == id }?.asKey()
    }

    override suspend fun delete(subject: String): Boolean = repo.remove(docKey(subject)).count > 0

    private fun DepotSubjectKeyRecord.asKey() = DepotSubjectKey(id = keyId, subject = subject, key = Base64.getDecoder().decode(key))

    /** A document key both databases take, the same for one subject: the SHA-256 of the subject, in hex. */
    private fun docKey(subject: String): String =
        MessageDigest.getInstance("SHA-256").digest(subject.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private companion object {
        const val CREATE_ATTEMPTS = 5
        const val CREATE_RETRY_MS = 20L
    }
}
