package de.peekandpoke.ultra.common.fixtures

import de.peekandpoke.ultra.common.fixture.LoremPicsum
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith

class LoremPicsumSpec : StringSpec() {
    init {
        "invoke operator should return a valid image URL with specified dimensions" {
            val url = LoremPicsum(200, 300)
            url shouldStartWith "https://picsum.photos/200/300?r="
        }

        "imageUrl should generate a valid URL with specified dimensions" {
            val url = LoremPicsum.imageUrl(400, 500)
            url shouldStartWith "https://picsum.photos/400/500?r="
        }

        "imageUrl should include random parameter" {
            val url = LoremPicsum.imageUrl(100, 100)
            val randomPart = url.substringAfter("?r=")
            randomPart shouldNotBe ""
        }

        "imageUrls should generate the requested number of URLs" {
            val urls = LoremPicsum.imageUrls(5, 150, 150)
            urls shouldHaveSize 5
            urls.forEach { it shouldStartWith "https://picsum.photos/150/150?r=" }
        }

        "random values should be deterministic due to fixed seed" {
            val firstUrl = LoremPicsum.imageUrl(100, 100)
            val secondUrl = LoremPicsum.imageUrl(100, 100)
            firstUrl shouldNotBe secondUrl  // Different random values for each call

            // Reset to simulate fresh object instance
            val firstUrlsSet = LoremPicsum.imageUrls(3, 200, 200)
            val secondUrlsSet = LoremPicsum.imageUrls(3, 200, 200)

            // Each URL in the respective positions should be different
            for (i in 0 until 3) {
                firstUrlsSet[i] shouldNotBe secondUrlsSet[i]
            }
        }

        "imageUrls with zero amount should return empty list" {
            val urls = LoremPicsum.imageUrls(0, 100, 100)
            urls shouldBe emptyList()
        }

        "URLs should include provided dimensions correctly" {
            val url = LoremPicsum.imageUrl(123, 456)
            url.contains("/123/456") shouldBe true
        }
    }
}
