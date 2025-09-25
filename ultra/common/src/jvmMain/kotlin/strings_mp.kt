package de.peekandpoke.ultra.common

import java.nio.charset.StandardCharsets

private val charMap = mutableMapOf<Int, String>()

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
).map { it.code }.toSet()

actual fun String.encodeUriComponent(): String {
    return this.toByteArray(StandardCharsets.UTF_8).joinToString("") { byte ->
        val unsignedByte = byte.toInt() and 0xFF

        charMap.getOrPut(unsignedByte) {
            when {
                // Check if this byte represents an unreserved ASCII character
                (unsignedByte in 65..90) ||  // A-Z
                        (unsignedByte in 97..122) || // a-z
                        (unsignedByte in 48..57) ||  // 0-9
                        (unsignedByte in specialChars)
                    -> unsignedByte.toChar().toString()
                // Everything else gets percent-encoded
                else -> "%${unsignedByte.toString(16).uppercase().padStart(2, '0')}"
            }
        }
    }
}

actual fun String.decodeUriComponent(): String {
    val result = StringBuilder()
    var i = 0

    while (i < length) {
        when {
            this[i] == '%' -> {
                // Collect consecutive percent-encoded bytes
                val bytes = mutableListOf<Byte>()

                while (i < length && this[i] == '%' && i + 2 < length) {
                    try {
                        val hex = substring(i + 1, i + 3)
                        val byte = hex.toInt(16).toByte()
                        bytes.add(byte)
                        i += 3
                    } catch (e: NumberFormatException) {
                        // Invalid hex sequence, break and treat % as literal
                        break
                    }
                }

                if (bytes.isNotEmpty()) {
                    // Convert the collected bytes to UTF-8 string
                    val utf8String = String(bytes.toByteArray(), StandardCharsets.UTF_8)
                    result.append(utf8String)
                } else {
                    // No valid percent encoding found, append the % character
                    result.append('%')
                    i++
                }
            }

            else -> {
                // All other characters (including '+') are treated literally
                result.append(this[i])
                i++
            }
        }
    }

    return result.toString()
}
