package de.peekandpoke.ultra.common

import java.math.BigInteger
import java.security.MessageDigest

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
