package de.peekandpoke.ultra.common.markup.images

/**
 * Cloudinary specific [ImageSrcSet] generator
 */
object CloudinaryImageSrcSetGenerator : ImageSrcSetGenerator {

    override fun generate(src: String, sizes: ImageSizes): ImageSrcSet? {

        val img = CloudinaryImage.fromUrl(src)
            ?.autoFormat()
            ?.autoDpr()
            ?.autoQuality() ?: return null

        return ImageSrcSet(
            src,
            sizes.sizes.map {
                ImageSrcSet.Entry(
                    img.transformWidth(it.px).url, it.px
                )
            }
        )
    }
}
