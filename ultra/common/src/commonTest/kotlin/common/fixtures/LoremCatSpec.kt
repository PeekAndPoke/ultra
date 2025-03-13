package de.peekandpoke.ultra.common.fixtures

import de.peekandpoke.ultra.common.fixture.LoremCat
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class LoremCatSpec : StringSpec() {
    init {
        "invoke operator should return a valid image URL with specified dimensions" {
            val url = LoremCat(200, 300)
            url shouldBe "https://api.images.cat/200/300"
        }

        "invoke operator should include uuid when provided" {
            val url = LoremCat(200, 300, "test-uuid")
            url shouldBe "https://api.images.cat/200/300/test-uuid"
        }

        "imageUrl should generate a valid URL with specified dimensions" {
            val url = LoremCat.imageUrl(400, 500)
            url shouldBe "https://api.images.cat/400/500"
        }

        "imageUrl should include uuid when provided" {
            val url = LoremCat.imageUrl(100, 100, "my-special-cat")
            url shouldBe "https://api.images.cat/100/100/my-special-cat"
        }

        "imageUrls should generate the requested number of URLs" {
            val urls = LoremCat.imageUrls(5, 150, 150)
            urls shouldHaveSize 5
            urls.forEach { it shouldBe "https://api.images.cat/150/150" }
        }

        "imageUrls with zero amount should return empty list" {
            val urls = LoremCat.imageUrls(0, 100, 100)
            urls shouldBe emptyList()
        }

        "URLs should include provided dimensions correctly" {
            val url = LoremCat.imageUrl(123, 456)
            url shouldBe "https://api.images.cat/123/456"
        }

        "All URLs in imageUrls should be identical when no uuid is provided" {
            val urls = LoremCat.imageUrls(3, 200, 200)
            urls.distinct() shouldHaveSize 1
            urls[0] shouldBe "https://api.images.cat/200/200"
        }

        "Special characters in uuid should be properly included in URL" {
            val url = LoremCat.imageUrl(100, 100, "cat&kitten")
            url shouldBe "https://api.images.cat/100/100/cat&kitten"
        }

        "Empty string uuid should be handled correctly" {
            val url = LoremCat.imageUrl(100, 100, "")
            url shouldBe "https://api.images.cat/100/100/"
        }
    }
}
