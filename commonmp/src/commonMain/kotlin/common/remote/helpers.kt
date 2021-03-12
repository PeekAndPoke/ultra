package de.peekandpoke.ultra.common.remote

external fun encodeURIComponent(value: String): String

/**
 * TODO: TESTS
 *
 * Appends all none empty [params] as url parameters to the given [uri]
 */
fun buildUri(uri: String, params: Map<String, String?> = emptyMap()): String {

    @Suppress("UNCHECKED_CAST")
    val filtered = params.filter { (_, v) -> v != null && v.isNotBlank() } as Map<String, String>

    if (filtered.isEmpty()) {
        return uri
    }

    // We replace all param placeholders in the uri
    // And we collect all the params that are not part of the uri
    val paramsNotInUri = mutableMapOf<String, String>()

    val uriReplaced = filtered.entries.fold(uri) { acc, (k, v) ->
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
