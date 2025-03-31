package de.peekandpoke.funktor.staticweb.resources

/**
 * A single web resource
 */
data class WebResource(val uri: String, val cacheKey: String? = null, val integrity: String? = null) {

    val fullUri = when (cacheKey) {
        null -> uri
        else -> "$uri?$cacheKey"
    }
}
