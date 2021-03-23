package de.peekandpoke.ultra.common.markup.images

/**
 * Represents an image src set
 */
data class ImageSrcSet(val url: String, val entries: List<Entry>) {

    /**
     * An entry in the src set
     */
    data class Entry(val url: String, val maxWidth: Int)

    companion object {
        fun of(url: String) = ImageSrcSet(url, emptyList())
    }

    /**
     * Renders the [ImageSrcSet] as a string that is understood by the browser in the img::srcset attribute
     */
    fun render(): String {
        return entries.joinToString(",") { "${it.url} ${it.maxWidth}w" }
    }
}
