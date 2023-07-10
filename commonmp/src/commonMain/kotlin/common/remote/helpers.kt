package de.peekandpoke.ultra.common.remote

/**
 * Builds an uri from the given [pattern] and [params].
 */
fun buildUri(pattern: String, vararg params: Pair<String, String?>): String {
    return buildUri(pattern, params.toMap())
}

/**
 * Builds an uri from the given [pattern] and [params].
 *
 * It tries to replace all parameters names with value by replacing
 * {PARAM_NAME} with its string value.
 *
 * All parameters that are not found in the uri [pattern] will be appended
 * as query parameters.
 *
 * Parameters that have a null value or a blank string will NOT be included
 */
fun buildUri(pattern: String, params: Map<String, String?> = emptyMap()): String {

    val emptyParams = uriToParamsCache.getOrPut(pattern) {
        placeholderRegex
            .findAll(pattern)
            .map { it.groupValues[1] }
            .associateWith { "" }
    }

    @Suppress("UNCHECKED_CAST")
    val cleaned = emptyParams.plus(
        params.filter { (_, v) -> !v.isNullOrBlank() } as Map<String, String>
    )

    if (cleaned.isEmpty()) {
        return pattern
    }

    // We replace all param placeholders in the uri
    // And we collect all the params that are not part of the uri
    val paramsNotInUri = mutableMapOf<String, String>()

    val uriReplaced = cleaned.entries.fold(pattern) { acc, (k, v) ->
        if (acc.contains("{$k}")) {
            acc.replace("{$k}", encodeURIComponent(v))
        } else {
            paramsNotInUri[k] = v
            acc
        }
    }

    return when (paramsNotInUri.isEmpty()) {
        true -> uriReplaced

        else -> "$uriReplaced?" + paramsNotInUri
            .map { (k, v) -> encodeURIComponent(k) + "=" + encodeURIComponent(v) }
            .joinToString("&")
    }
}

private val uriToParamsCache = mutableMapOf<String, Map<String, String>>()

private val placeholderRegex = "\\{([^}]*)\\}".toRegex()
