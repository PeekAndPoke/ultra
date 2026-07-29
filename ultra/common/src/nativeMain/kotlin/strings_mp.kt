package io.peekandpoke.ultra.common

/** Byte-to-output-fragment cache, at most 256 entries. Not synchronised. */
private val charMap = mutableMapOf<Int, String>()

/** Unreserved punctuation that `encodeURIComponent` leaves untouched. */
private val specialChars = listOf(
    '!',
    '\'',
    '(',
    ')',
    '*',
    '-',
    '.',
    '_',
    '(',
    ')',
    '~',
).map { it.code }.toSet()

/**
 * An unpaired surrogate is encoded as `%EF%BF%BD` (U+FFFD), whereas JVM yields `%3F` and JS throws.
 */
actual fun String.encodeUriComponent(): String {
    return this.encodeToByteArray().joinToString("") { byte ->
        val unsignedByte = byte.toInt() and 0xFF

        charMap.getOrPut(unsignedByte) {
            when {
                (unsignedByte in 65..90) || // A-Z
                        (unsignedByte in 97..122) || // a-z
                        (unsignedByte in 48..57) || // 0-9
                        (unsignedByte in specialChars)
                    -> unsignedByte.toChar().toString()

                else -> "%${unsignedByte.toString(16).uppercase().padStart(2, '0')}"
            }
        }
    }
}

/**
 * Malformed escapes are passed through as literal text and undecodable bytes become U+FFFD;
 * JS throws `URIError` for the same input.
 */
actual fun String.decodeUriComponent(): String {
    val result = StringBuilder()
    var i = 0

    while (i < length) {
        when {
            this[i] == '%' -> {
                val bytes = mutableListOf<Byte>()

                while (i < length && this[i] == '%' && i + 2 < length) {
                    try {
                        val hex = substring(i + 1, i + 3)
                        val byte = hex.toInt(16).toByte()
                        bytes.add(byte)
                        i += 3
                    } catch (@Suppress("SwallowedException") e: NumberFormatException) {
                        break
                    }
                }

                if (bytes.isNotEmpty()) {
                    val utf8String = bytes.toByteArray().decodeToString()
                    result.append(utf8String)
                } else {
                    result.append('%')
                    i++
                }
            }

            else -> {
                result.append(this[i])
                i++
            }
        }
    }

    return result.toString()
}
