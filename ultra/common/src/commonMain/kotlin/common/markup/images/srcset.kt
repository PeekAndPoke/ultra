package de.peekandpoke.ultra.common.markup.images

private val srcSetCache = mutableMapOf<SrcSetCacheEntry, ImageSrcSet>()

private data class SrcSetCacheEntry(
    var url: String,
    var sizes: ImageSizes
)

/**
 * Generates an [ImageSrcSet] from the given [src] and [sizes].
 *
 * Automatically tries to detect the following image hosting providers:
 * - Cloudinary
 *
 * Of the detection fails an empty [ImageSrcSet] is returned.
 */
fun createSrcSet(src: String, sizes: ImageSizes): ImageSrcSet = srcSetCache
    .getOrPut(SrcSetCacheEntry(src, sizes)) {

        CloudinaryImageSrcSetGenerator.generate(src, sizes)
            ?: ImageSrcSet.of(src)
    }
