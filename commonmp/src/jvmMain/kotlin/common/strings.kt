@file:JvmName("StringJvmKt")

package de.peekandpoke.ultra.common

import java.net.URLEncoder

/**
 * Appends url parameters the string
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
 */
fun String.toUri(queryParams: List<Pair<String, String>>) = toUri(queryParams.toMap())

/**
 * Appends url parameters the string
 */
fun String.toUri(vararg queryParams: Pair<String, String>) = toUri(queryParams.toMap())
