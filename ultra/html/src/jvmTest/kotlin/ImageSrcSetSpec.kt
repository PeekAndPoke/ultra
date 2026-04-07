package io.peekandpoke.ultra.html

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ImageSrcSetSpec : FreeSpec() {

    init {
        "ImageSrcSet.of" - {

            "Creates an ImageSrcSet with the given url and empty entries" {

                val result = ImageSrcSet.of("https://example.com/image.jpg")

                result.url shouldBe "https://example.com/image.jpg"
                result.entries.shouldBeEmpty()
            }
        }

        "render" - {

            "Empty entries renders empty string" {

                val srcSet = ImageSrcSet.of("https://example.com/image.jpg")

                srcSet.render() shouldBe ""
            }

            "Single entry renders correctly" {

                val srcSet = ImageSrcSet(
                    url = "https://example.com/image.jpg",
                    entries = listOf(
                        ImageSrcSet.Entry(url = "https://example.com/image_320.jpg", maxWidth = 320),
                    )
                )

                srcSet.render() shouldBe "https://example.com/image_320.jpg 320w"
            }

            "Multiple entries renders comma-separated" {

                val srcSet = ImageSrcSet(
                    url = "https://example.com/image.jpg",
                    entries = listOf(
                        ImageSrcSet.Entry(url = "https://example.com/image_320.jpg", maxWidth = 320),
                        ImageSrcSet.Entry(url = "https://example.com/image_640.jpg", maxWidth = 640),
                        ImageSrcSet.Entry(url = "https://example.com/image_1024.jpg", maxWidth = 1024),
                    )
                )

                srcSet.render() shouldBe
                        "https://example.com/image_320.jpg 320w," +
                        "https://example.com/image_640.jpg 640w," +
                        "https://example.com/image_1024.jpg 1024w"
            }
        }

        "auto" - {

            "Delegates to createSrcSet" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val result = ImageSrcSet.auto(url, ImageSizes.default)

                result.url shouldBe url
                result.entries.size shouldBe ImageSizes.default.sizes.size
            }
        }
    }
}
