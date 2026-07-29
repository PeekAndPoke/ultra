package io.peekandpoke.ultra.common

import java.nio.charset.Charset
import java.util.Base64

/**
 * Character lookup for [toHex]
 */
private val hexArray = "0123456789abcdef".toCharArray()

/**
 * Converts the ByteArray into a lowercase hex string of exactly `size * 2` characters.
 */
fun ByteArray.toHex(): String {

    val hexChars = CharArray(size * 2)

    forEachIndexed { idx, byte ->

        @Suppress("EXPERIMENTAL_API_USAGE")
        val v = byte.toUByte().toInt()

        hexChars[idx * 2] = hexArray[v ushr 4]
        hexChars[idx * 2 + 1] = hexArray[v and 0x0F]
    }

    return String(hexChars)
}

/**
 * Encodes the byte array as a base64 string, using the standard RFC 4648 alphabet.
 *
 * The result may contain `+`, `/` and `=`, so it is neither url- nor filename-safe. Percent-encode
 * it before putting it into a uri.
 */
fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

/**
 * Encodes the string as a base64 string, turning it into bytes with [charset] first.
 *
 * See `ByteArray.toBase64` for the alphabet used.
 */
fun String.toBase64(charset: Charset = Charsets.UTF_8): String = toByteArray(charset).toBase64()

/**
 * Decodes the standard-alphabet base64 string. Missing trailing padding is tolerated.
 *
 * @throws IllegalArgumentException if the input is not valid base64 - illegal characters (this
 *   includes whitespace, line breaks and the url-safe `-` and `_`) or a broken ending unit. Callers
 *   that decode untrusted input must handle this.
 */
fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)

/**
 * Decodes the standard-alphabet base64 string, or returns null when it is not valid base64.
 *
 * The lenient counterpart of [fromBase64] — prefer it for input that can legitimately be malformed,
 * such as a request parameter or a stored value of unknown vintage.
 */
fun String.fromBase64OrNull(): ByteArray? = try {
    fromBase64()
} catch (_: IllegalArgumentException) {
    null
}
