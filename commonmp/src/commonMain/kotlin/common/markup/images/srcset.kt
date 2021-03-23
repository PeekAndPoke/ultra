package de.peekandpoke.ultra.common.markup.images

private val srcSetCache = mutableMapOf<SrcSetCacheEntry, ImageSrcSet?>()

private data class SrcSetCacheEntry(
    var url: String,
    var sizes: ImageSizes
)

fun createSrcSet(url: String, sizes: ImageSizes): ImageSrcSet? {
    return srcSetCache.getOrPut(SrcSetCacheEntry(url, sizes)) {
        createSrcSetForCloudinaryImage(url, sizes)
    }
}

private fun createSrcSetForCloudinaryImage(url: String, sizes: ImageSizes): ImageSrcSet? {
    val img = CloudinaryImage.fromUrl(url)
        ?.autoFormat()
        ?.autoDpr()
        ?.autoQuality() ?: return null

    return ImageSrcSet(
        url,
        sizes.sizes.map {
            ImageSrcSet.Entry(
                img.transformWidth(it.px).url, it.px
            )
        }
    )
}
