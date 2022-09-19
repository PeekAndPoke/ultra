package de.peekandpoke.ultra.common

import java.nio.charset.Charset
import java.util.*

/**
 * Character lookup for [toHex]
 */
private val hexArray = "0123456789abcdef".toCharArray()

/**
 * Converts the ByteArray into a hex string
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
 * Encodes the byte array as a base64 string
 */
fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

/**
 * Encodes the string as base64 string
 */
fun String.toBase64(charset: Charset = Charsets.UTF_8): String = toByteArray(charset).toBase64()

/**
 * Decodes the base64 string
 */
fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this)
