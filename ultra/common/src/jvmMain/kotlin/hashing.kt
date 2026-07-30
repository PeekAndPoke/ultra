package io.peekandpoke.ultra.common

import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

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

/**
 * Calculates the HMAC-SHA-256 of this byte array under [key], returning the raw 32 digest bytes.
 *
 * Use this, not a plain digest, whenever a value has to be authenticated with a secret. `H(data)`
 * with the secret merely concatenated is a hand-rolled construction whose security rests on
 * collision resistance; HMAC is designed for the job and its strength does not.
 *
 * Render the result with [toHex] or [toBase64], and compare two MACs with
 * `MessageDigest.isEqual` rather than `==` so the comparison stays constant-time.
 */
fun ByteArray.hmacSha256(key: ByteArray): ByteArray = hmac("HmacSHA256", key)

/** HMAC-SHA-256 of the utf-8 encoded string under the utf-8 encoded [key]. See `ByteArray.hmacSha256`. */
fun String.hmacSha256(key: String): ByteArray = toByteArray().hmacSha256(key.toByteArray())

/**
 * Calculates the HMAC-SHA-384 of this byte array under [key], returning the raw 48 digest bytes.
 *
 * See `ByteArray.hmacSha256` for when to reach for a MAC rather than a digest.
 */
fun ByteArray.hmacSha384(key: ByteArray): ByteArray = hmac("HmacSHA384", key)

/** HMAC-SHA-384 of the utf-8 encoded string under the utf-8 encoded [key]. See `ByteArray.hmacSha384`. */
fun String.hmacSha384(key: String): ByteArray = toByteArray().hmacSha384(key.toByteArray())

/**
 * Runs [algorithm] over this byte array under [key].
 *
 * @throws IllegalArgumentException for an empty [key] — `Mac` rejects it, and an empty secret means
 *   the MAC authenticates nothing.
 */
private fun ByteArray.hmac(algorithm: String, key: ByteArray): ByteArray {
    require(key.isNotEmpty()) { "The HMAC key must not be empty" }

    return Mac.getInstance(algorithm).apply {
        init(SecretKeySpec(key, algorithm))
    }.doFinal(this)
}
