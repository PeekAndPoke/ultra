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

        /**
         * Generates an [ImageSrcSet] from the given [src] and [sizes].
         *
         * Automatically tries to detect the following image hosting providers:
         * - Cloudinary
         *
         * Of the detection fails an empty [ImageSrcSet] is returned.
         */
        fun auto(src: String, sizes: ImageSizes): ImageSrcSet {
            return CloudinaryImageSrcSetGenerator.generate(src, sizes)
                ?: of(src)
        }
    }

    /**
     * Renders the [ImageSrcSet] as a string that is understood by the browser in the img::srcset attribute
     */
    fun render(): String {
        return entries.joinToString(",") { "${it.url} ${it.maxWidth}w" }
    }
}
