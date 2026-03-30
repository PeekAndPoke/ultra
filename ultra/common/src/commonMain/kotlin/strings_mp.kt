package io.peekandpoke.ultra.common

/**
 * Encodes this string as a URI component, percent-encoding special characters.
 *
 * Equivalent to JavaScript's `encodeURIComponent()`.
 */
expect fun String.encodeUriComponent(): String

/**
 * Decodes a percent-encoded URI component string back to its original form.
 *
 * Equivalent to JavaScript's `decodeURIComponent()`.
 */
expect fun String.decodeUriComponent(): String
