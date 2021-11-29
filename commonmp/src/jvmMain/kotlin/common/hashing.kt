package de.peekandpoke.ultra.common

import java.math.BigInteger
import java.nio.charset.Charset
import java.security.MessageDigest
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

/**
 * Calculates the md5 hash for the ByteArray
 */
fun ByteArray.md5(): String {
    val md = MessageDigest.getInstance("MD5")

    val radix = 16
    val md5Length = 32

    return BigInteger(1, md.digest(this))
        .toString(radix)
        .padStart(md5Length, '0')
}

/**
 * Calculates the md5 hash of the string
 */
fun String.md5(): String = toByteArray().md5()

/**
 * Calculates the sha256 hash for byte array
 */
fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)

/**
 * Calculates the sha256 hash for the string
 */
fun String.sha256(): ByteArray = toByteArray().sha256()

/**
 * Calculates the sha384 hash for byte array
 */
fun ByteArray.sha384(): ByteArray = MessageDigest.getInstance("SHA-384").digest(this)

/**
 * Calculates the sha384 hash for the string
 */
fun String.sha384(): ByteArray = toByteArray().sha384()
