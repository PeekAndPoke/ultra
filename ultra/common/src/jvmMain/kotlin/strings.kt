@file:JvmName("StringJvmKt")

package io.peekandpoke.ultra.common

import java.net.URLEncoder

/**
 * Appends url parameters the string
 *
 * An empty [queryParams] leaves the string untouched. The separator is `?`, or `&` if the string
 * already contains a `?` anywhere — this is a plain string operation, not URL parsing, so a string
 * carrying a `#fragment` gets its parameters appended INSIDE the fragment.
 *
 * Keys and values are form-encoded (`URLEncoder`), which encodes a space as `+`.
 */
fun String.toUri(queryParams: Map<String, String>) = when {
    queryParams.isEmpty() -> this

    else -> when {
        contains("?") -> "$this&"
        else -> "$this?"
    } + queryParams
        .map { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        .joinToString("&")
}

/**
 * Appends url parameters the string
 *
 * Goes through a `Map`, so repeating a key keeps only its LAST value — this overload cannot express
 * `?tag=a&tag=b`.
 */
fun String.toUri(queryParams: List<Pair<String, String>>) = toUri(queryParams.toMap())

/**
 * Appends url parameters the string
 *
 * Goes through a `Map`, so repeating a key keeps only its LAST value.
 */
fun String.toUri(vararg queryParams: Pair<String, String>) = toUri(queryParams.toMap())
