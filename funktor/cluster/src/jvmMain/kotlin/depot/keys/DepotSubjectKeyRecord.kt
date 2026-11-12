package io.peekandpoke.funktor.cluster.depot.keys

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Vault

/**
 * The stored key of one data subject. A credential: it is kept out of insights, logs and API output.
 *
 * The document's key is derived from the [subject], so one subject has one record.
 */
@Vault
data class DepotSubjectKeyRecord(
    /** The id files name their key by; a new one with every key, so an erased key is never found again. */
    val keyId: String,
    /** The subject id's value. */
    val subject: String,
    /** The key, base64. */
    val key: String,
    val createdAt: MpInstant,
    /** Set when the subject was erased: the record then holds no key and no subject, only that it was erased. */
    val erasedAt: MpInstant? = null,
) {
    /** Without the key: a log line or a failed assertion prints this. */
    override fun toString(): String = "DepotSubjectKeyRecord(keyId=$keyId, subject=$subject, createdAt=$createdAt, erasedAt=$erasedAt)"
}
