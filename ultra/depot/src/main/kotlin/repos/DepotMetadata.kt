package io.peekandpoke.ultra.depot.repos

import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import java.net.URLEncoder

/** Thrown for metadata a depot repository refuses: a bad or reserved key, or too much of it. */
class DepotInvalidMetadataException(message: String) : IllegalArgumentException(message)

/**
 * The rules for a file's metadata, a free map of strings, the same in every repository:
 *
 * - a key is 1 to 64 characters of `a-z`, `0-9` and `-` (S3 lowercases header names);
 * - keys starting with `depot-` are the depot's own and refused from a caller;
 * - a [MIME_TYPE] entry looks like `type/subtype`;
 * - all keys and values together stay within [MAX_BYTES], S3's limit for user metadata, measured as S3 stores
 *   them (values percent-encoded).
 */
object DepotMetadata {
    /** Prefix of the keys the depot writes itself. */
    const val RESERVED_PREFIX = "depot-"

    /** The data subject's id, written with every file. */
    const val SUBJECT = "depot-subject"

    /**
     * The file's mime type, by convention. It must look like `type/subtype`. The S3 repository also sets it as the
     * object's content type, which is what a bucket served to browsers answers with: an app that takes it from a user
     * decides which types it allows.
     */
    const val MIME_TYPE = "mime-type"

    private val mimeRule = Regex("[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+")

    /** S3's limit for the user metadata of one object, in bytes. */
    const val MAX_BYTES = 2048

    private val keyRule = Regex("[a-z0-9-]{1,64}")

    /** A value as S3 carries it in a header: percent-encoded UTF-8. */
    fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)

    /** Checks a caller's metadata and returns it; see the rules above. */
    fun validate(meta: Map<String, String>): Map<String, String> = meta.also {
        meta[MIME_TYPE]?.let { mime ->
            if (!mimeRule.matches(mime)) throw DepotInvalidMetadataException("Invalid mime type: expected type/subtype")
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

    /** What is stored for a write: the caller's [meta], checked, with the [subject]'s id, within the size limit. */
    fun stored(subject: DepotSubjectId<*>, meta: Map<String, String>): Map<String, String> =
        validateSize(validate(meta) + (SUBJECT to subject.value))

    /** Checks the size of everything that is stored, the depot's own entries included. */
    fun validateSize(stored: Map<String, String>): Map<String, String> = stored.also {
        val bytes = stored.entries.sumOf { (key, value) -> key.length + encode(value).length }

        if (bytes > MAX_BYTES) {
            throw DepotInvalidMetadataException("Metadata too large: $bytes bytes, the limit is $MAX_BYTES")
        }
    }
}
