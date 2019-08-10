package de.peekandpoke.ultra.common

import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

/**
 * Encodes the byte array as a base64 string
 */
fun ByteArray.base64(): String = Base64.getEncoder().encodeToString(this)

fun ByteArray.md5(): String {
    val md = MessageDigest.getInstance("MD5")

    return BigInteger(1, md.digest(this))
        .toString(16)
        .padStart(32, '0')
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
