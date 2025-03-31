package de.peekandpoke.funktor.staticweb.templating

import de.peekandpoke.ultra.common.markup.images.ImageSizes
import de.peekandpoke.ultra.common.markup.images.ImageSrcSet
import de.peekandpoke.ultra.common.markup.images.Transparent2x1PngBase64
import kotlinx.html.FlowContent
import kotlinx.html.IMG
import kotlinx.html.img

private data class ImageSrcSetCacheEntry(val src: String, val sizes: ImageSizes)

private val imageSrcSetCache = mutableMapOf<ImageSrcSetCacheEntry, ImageSrcSet>()

/**
 * Renders an image tag with srcset and sizes
 *
 * see https://developer.mozilla.org/en-US/docs/Learn/HTML/Multimedia_and_embedding/Responsive_images
 */
fun FlowContent.srcSetImage(src: String, alt: String, sizes: ImageSizes = ImageSizes.default) {

    if (src.isNotBlank()) {

        val imageSrcSet = imageSrcSetCache.getOrPut(ImageSrcSetCacheEntry(src, sizes)) {
            ImageSrcSet.auto(src, sizes)
        }

        img(src = imageSrcSet.url, alt = alt) {

            if (imageSrcSet.entries.isNotEmpty()) {
                // sizes attribute for image cdn
                attributes["sizes"] = sizes.render()

                // src set
                if (imageSrcSet.entries.isNotEmpty()) {
                    attributes["srcset"] =
                        imageSrcSet.entries.joinToString(",") { "${it.url} ${it.maxWidth}w" }
                }
            }
        }

    } else {
        img(src = Transparent2x1PngBase64, alt = alt)
    }
}

fun FlowContent.lazySrcSetImage(
    src: String,
    alt: String,
    sizes: ImageSizes = ImageSizes.default,
    modify: (IMG.() -> Unit)? = null,
) {

    if (src.isNotBlank()) {

        val imageSrcSet = imageSrcSetCache.getOrPut(ImageSrcSetCacheEntry(src, sizes)) {
            ImageSrcSet.auto(src, sizes)
        }

        when (imageSrcSet.entries.isEmpty()) {
            true -> img(src = imageSrcSet.url, alt = alt) { modify?.let { it() } }

            else -> img(classes = "lazyload", alt = alt) {
                modify?.let { it() }
                // dummy source before the real image loads (2x1 transparent png base64 encoded)
                this.src = Transparent2x1PngBase64
                // source
                attributes["data-src"] = imageSrcSet.url
                // srcset and sizes
                if (imageSrcSet.entries.isNotEmpty()) {
                    attributes["data-sizes"] = sizes.render()
                    attributes["data-srcset"] = imageSrcSet.render()
                }
            }
        }
    } else {
        img(src = Transparent2x1PngBase64, alt = alt)
    }
}
