package io.peekandpoke.ultra.depot.repos

import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import java.net.URLEncoder
import java.security.MessageDigest

/** Thrown for metadata a depot repository refuses: a bad or reserved key, or too much of it. */
class DepotInvalidMetadataException(message: String) : IllegalArgumentException(message)

/**
 * The rules for a file's metadata, a free map of strings, the same in every repository. Metadata is stored in
 * clear and stays readable when a subject's files are made unreadable: it holds no personal data (no names, no
 * titles that name a person).
 *
 * - a key is 1 to 64 characters of `a-z`, `0-9` and `-` (S3 lowercases header names);
 * - keys starting with `depot-` are the depot's own and refused from a caller;
 * - the depot adds the subject's id ([SUBJECT]) and the content's hash ([SHA256]), which count towards the size;
 * - a [MIME_TYPE] entry looks like `type/subtype`, parameters allowed (`; charset=utf-8`);
 * - all keys and values together stay within [MAX_BYTES], S3's limit for user metadata, measured as S3 stores
 *   them (values percent-encoded).
 */
object DepotMetadata {
    /** Prefix of the keys the depot writes itself. */
    const val RESERVED_PREFIX = "depot-"

    /** The data subject's id, written with every file. */
    const val SUBJECT = "depot-subject"

    /** The SHA-256 of the bytes as they are stored, in hex, written with every file; see [DepotRepository.verify]. */
    const val SHA256 = "depot-sha256"

    /** The encryption method of an encrypted file, as `DepotEncryption.name`. */
    const val ENC = "depot-enc"

    /** The id of the subject's key that wraps an encrypted file's own key. */
    const val KEY = "depot-key"

    /** What the encryption method needs besides the key to read an encrypted file. */
    const val ENC_PARAMS = "depot-enc-params"

    /** The entries only the depot reads: left out of what `getMeta` of an encrypting repository returns. */
    val internal: Set<String> = setOf(KEY, ENC_PARAMS)

    /**
     * The file's mime type, by convention. It must look like `type/subtype`, with parameters if any. The S3 repository also sets it as the
     * object's content type, which is what a bucket served to browsers answers with: an app that takes it from a user
     * decides which types it allows.
     */
    const val MIME_TYPE = "mime-type"

    private const val TOKEN = "[A-Za-z0-9!#$&^_.+-]+"

    /** `type/subtype`, then any number of `; name=value` with a token or a quoted value; never a control character. */
    private val mimeRule = Regex("$TOKEN/$TOKEN( *; *$TOKEN=($TOKEN|\"[^\"\\\\\\p{Cntrl}]*\"))*")

    /** S3's limit for the user metadata of one object, in bytes. */
    const val MAX_BYTES = 2048

    private val keyRule = Regex("[a-z0-9-]{1,64}")

    /** A value as S3 carries it in a header: percent-encoded UTF-8, a space as `%20`. */
    fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    /** The subject's id as recorded in a file's [meta]; null when none is. Read it here, not from the map. */
    fun subjectOf(meta: Map<String, String>): String? = meta[SUBJECT]

    /** The caller's own entries of a file's metadata, without the depot's: what a write takes back. */
    fun withoutReserved(meta: Map<String, String>): Map<String, String> = meta.filterKeys { !it.startsWith(RESERVED_PREFIX) }

    /** Checks a caller's metadata and returns it; see the rules above. */
    fun validate(meta: Map<String, String>): Map<String, String> = meta.also {
        meta[MIME_TYPE]?.let { mime ->
            if (!mimeRule.matches(mime)) throw DepotInvalidMetadataException("Invalid mime type: expected type/subtype, with parameters if any")
        }

        meta.keys.forEach { key ->
            if (!keyRule.matches(key)) {
                throw DepotInvalidMetadataException("Invalid metadata key: 1 to 64 characters of a-z, 0-9 and '-'")
            }
            if (key.startsWith(RESERVED_PREFIX)) {
                throw DepotInvalidMetadataException("Metadata keys starting with '$RESERVED_PREFIX' are reserved")
            }
        }
    }

    /**
     * What is stored for a write: the caller's [meta], checked, with the [subject]'s id and the [content]'s hash,
     * within the size limit.
     */
    fun stored(subject: DepotSubjectId<*>, meta: Map<String, String>, content: ByteArray): Map<String, String> =
        validateSize(validate(meta) + (SUBJECT to subject.value) + (SHA256 to sha256(content)))

    /** The SHA-256 of [content], in lowercase hex. */
    fun sha256(content: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }

    /** Checks what a middleware stores as it is ([DepotRepository.putRaw]): the keys' form and the size. */
    fun validateRaw(stored: Map<String, String>): Map<String, String> = validateSize(stored).also {
        stored.keys.forEach { key ->
            if (!keyRule.matches(key)) {
                throw DepotInvalidMetadataException("Invalid metadata key: 1 to 64 characters of a-z, 0-9 and '-'")
            }
        }
    }

    /** Checks the size of everything that is stored, the depot's own entries included. */
    fun validateSize(stored: Map<String, String>): Map<String, String> = stored.also {
        val bytes = stored.entries.sumOf { (key, value) -> key.length + encode(value).length }

        if (bytes > MAX_BYTES) {
            throw DepotInvalidMetadataException("Metadata too large: $bytes bytes, the limit is $MAX_BYTES")
        }
    }
}
