package de.peekandpoke.ultra.html

/**
 * Image src set generator generate an [ImageSrcSet] from a source and [ImageSizes]
 */
interface ImageSrcSetGenerator {
    fun generate(src: String, sizes: ImageSizes): ImageSrcSet?
}
