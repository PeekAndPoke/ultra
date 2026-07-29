package io.peekandpoke.ultra.common

import java.math.BigInteger
import java.security.MessageDigest

/**
 * Calculates the md5 hash for the ByteArray as a lowercase, zero-padded 32 character hex string.
 *
 * MD5 is broken with respect to collisions: use it for cache keys and fingerprints, never for
 * signatures, tokens or passwords.
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
 * Calculates the md5 hash of the utf-8 encoded string. See `ByteArray.md5`.
 */
fun String.md5(): String = toByteArray().md5()

/**
 * Calculates the sha256 hash for byte array and returns the raw 32 digest bytes.
 *
 * Render them with [toHex] or [toBase64]. This is a plain digest, not a MAC and not a password
 * KDF - use HMAC or `PasswordHasher` for those.
 */
fun ByteArray.sha256(): ByteArray = MessageDigest.getInstance("SHA-256").digest(this)

/**
 * Calculates the sha256 hash of the utf-8 encoded string. See `ByteArray.sha256`.
 */
fun String.sha256(): ByteArray = toByteArray().sha256()

/**
 * Calculates the sha384 hash for byte array and returns the raw 48 digest bytes.
 *
 * Render them with [toHex] or [toBase64]. This is a plain digest, not a MAC and not a password
 * KDF - use HMAC or `PasswordHasher` for those.
 */
fun ByteArray.sha384(): ByteArray = MessageDigest.getInstance("SHA-384").digest(this)

/**
 * Calculates the sha384 hash of the utf-8 encoded string. See `ByteArray.sha384`.
 */
fun String.sha384(): ByteArray = toByteArray().sha384()
