package io.peekandpoke.ultra.common

import org.khronos.webgl.Uint8Array

/**
 * Encodes an uri
 *
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/encodeURIComponent*
 */
external fun encodeURIComponent(value: String): String

/**
 * Decodes an uri
 *
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/decodeURIComponent
 */
external fun decodeURIComponent(value: String): String

/**
 * Decodes a base64 encoded string("atob" should be read as "ASCII to binary").
 *
 * See https://developer.mozilla.org/en-US/docs/Glossary/Base64
 */
external fun atob(value: String): Uint8Array

/**
 * Encodes a base-64 encoded ASCII string from a "string" of binary data ("btoa" should be read as "binary to ASCII").
 *
 * See https://developer.mozilla.org/en-US/docs/Glossary/Base64
 */
external fun btoa(value: Uint8Array): String

/**
 * Throws `URIError` when the string contains an unpaired surrogate; the other platforms substitute
 * a replacement character instead.
 */
actual fun String.encodeUriComponent(): String {
    return encodeURIComponent(this)
}

/**
 * Throws `URIError` on malformed input (a dangling `%`, non-hex digits, or an escape sequence that
 * is not valid UTF-8); the other platforms recover silently.
 */
actual fun String.decodeUriComponent(): String {
    return decodeURIComponent(this)
}
