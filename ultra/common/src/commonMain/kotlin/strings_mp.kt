package io.peekandpoke.ultra.common

/**
 * Encodes this string as a URI component, percent-encoding special characters.
 *
 * Leaves `A-Z a-z 0-9 - _ . ! ~ * ' ( )` unencoded and percent-encodes every other UTF-8 byte
 * using upper-case hex, matching JavaScript's `encodeURIComponent()` for well-formed input.
 *
 * Input containing an unpaired surrogate is NOT handled uniformly: JS throws `URIError`,
 * JVM substitutes `?`, native substitutes U+FFFD.
 */
expect fun String.encodeUriComponent(): String

/**
 * Decodes a percent-encoded URI component string back to its original form.
 *
 * `+` is kept literally (it is not decoded to a space).
 *
 * Malformed input is NOT handled uniformly: JS throws `URIError`, while JVM and native
 * pass invalid escapes through unchanged and map undecodable bytes to U+FFFD.
 */
expect fun String.decodeUriComponent(): String
