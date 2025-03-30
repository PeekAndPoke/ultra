package de.peekandpoke.kraft.addons.files

import kotlinx.browser.document
import kotlinx.coroutines.asDeferred
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Image
import kotlin.js.Promise

private val convertibleImageMimeTypes = listOf(
    "image/bmp",
    "image/gif",
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/svg+xml",
    "image/tiff",
)

/**
 * Returns true when the given file is an image that can be converted
 */
fun LoadedFileBase64.isConvertibleImage(): Boolean = mimeType in convertibleImageMimeTypes

/**
 * Tries to compress the given file to a jpg.
 *
 * The compression is done when:
 * 1. The file is a supported image type
 * 2. The file is bigger then the desired [maxBytes]
 */
suspend fun LoadedFileBase64.compressImageToJpg(maxBytes: Long): LoadedFileBase64 {

    val original = this

    return when {
        dataUrl == null || !isConvertibleImage() || dataUrl.length < maxBytes -> original

        else -> {

            Promise<LoadedFileBase64> { resolve, _ ->
                val image = Image()
                image.src = dataUrl

                // IMPORTANT react on error to not get stuck on broken images
                image.onerror = { _, _, _, _, _ -> resolve(original) }

                image.onload = {
                    if (image.width <= 0 || image.height <= 0) {
                        resolve(original)
                    }

                    // OK now we have the image and can paint it onto a canvas
                    try {
                        console.log(image.width, image.height)

                        val canvas = document.createElement("canvas") as HTMLCanvasElement
                        canvas.width = image.width
                        canvas.height = image.height

                        val context = canvas.getContext("2d") as CanvasRenderingContext2D
                        context.drawImage(image, 0.0, 0.0)

                        // Try to encode the image multiple times until we get a good result
                        val result = (9 downTo 1).map { it / 10.0 }
                            // IMPORTANT: use a sequence to find the first fit without doing all the others
                            .asSequence()
                            .mapNotNull { quality ->
                                console.log("Using jpeg quality: $quality")
                                canvas.toDataURL("image/jpeg", quality).takeIf { it.length < maxBytes }
                            }.firstOrNull()

                        resolve(
                            LoadedFileBase64.of(file = file, dataUrl = result ?: dataUrl)
                        )

                    } catch (e: Throwable) {
                        resolve(original)
                    }
                }

            }.asDeferred().await()
        }
    }
}
