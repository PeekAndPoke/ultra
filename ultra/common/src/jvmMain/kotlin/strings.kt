@file:JvmName("StringJvmKt")

package io.peekandpoke.ultra.common

import java.net.URLEncoder

/**
 * Appends url parameters to the string.
 *
 * Repeated keys are preserved in order, so `listOf("tag" to "a", "tag" to "b")` yields
 * `?tag=a&tag=b`. A `#fragment` is split off and re-appended at the end, where it belongs — the
 * parameters go into the query, not into the fragment.
 *
 * An empty [queryParams] leaves the string untouched. Keys and values are form-encoded
 * (`URLEncoder`), which encodes a space as `+`.
 */
fun String.toUri(queryParams: List<Pair<String, String>>): String {
    if (queryParams.isEmpty()) {
        return this
    }

    // the fragment is not part of the query and has to stay behind it
    val fragmentAt = indexOf('#')
    val base = if (fragmentAt >= 0) substring(0, fragmentAt) else this
    val fragment = if (fragmentAt >= 0) substring(fragmentAt) else ""

    val separator = when {
        !base.contains('?') -> "?"
        base.endsWith('?') || base.endsWith('&') -> ""
        else -> "&"
    }

    val query = queryParams.joinToString("&") { (k, v) ->
        "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }

    return "$base$separator$query$fragment"
}

/**
 * Appends url parameters to the string.
 *
 * A `Map` cannot carry a repeated key — use the `List<Pair>` overload for `?tag=a&tag=b`.
 */
fun String.toUri(queryParams: Map<String, String>): String = toUri(queryParams.toList())

/**
 * Appends url parameters to the string.
 *
 * Repeated keys are preserved, unlike the `Map` overload.
 */
fun String.toUri(vararg queryParams: Pair<String, String>): String = toUri(queryParams.toList())
