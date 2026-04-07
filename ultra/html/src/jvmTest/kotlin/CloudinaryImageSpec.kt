package io.peekandpoke.ultra.html

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class CloudinaryImageSpec : FreeSpec() {

    init {
        "fromUrl" - {

            "Standard Cloudinary URL extracts correct parts" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/folder/image.jpg"

                val result = CloudinaryImage.fromUrl(url)

                result.shouldNotBeNull()
                result.protocol shouldBe "https"
                result.appId shouldBe "res.cloudinary.com/myapp"
                result.quality shouldBe emptyMap()
                result.transform shouldBe emptyMap()
                result.rest shouldBe listOf("v1234", "folder", "image.jpg")
            }

            "URL with quality keywords parses quality map" {

                val url = "https://res.cloudinary.com/myapp/image/upload/dpr_auto,f_auto/v1234/image.jpg"

                val result = CloudinaryImage.fromUrl(url)

                result.shouldNotBeNull()
                result.protocol shouldBe "https"
                result.appId shouldBe "res.cloudinary.com/myapp"
                result.quality shouldBe mapOf("dpr_auto" to null, "f_auto" to null)
                result.rest shouldBe listOf("v1234", "image.jpg")
            }

            "URL with all three quality keywords" {

                val url = "https://res.cloudinary.com/myapp/image/upload/f_auto,dpr_auto,q_auto/v1234/image.jpg"

                val result = CloudinaryImage.fromUrl(url)

                result.shouldNotBeNull()
                result.quality shouldBe mapOf("f_auto" to null, "dpr_auto" to null, "q_auto" to null)
                result.rest shouldBe listOf("v1234", "image.jpg")
            }

            "Non-Cloudinary URL returns null" {

                val url = "https://www.example.com/image.jpg"

                val result = CloudinaryImage.fromUrl(url)

                result.shouldBeNull()
            }

            "Empty string returns null" {

                val result = CloudinaryImage.fromUrl("")

                result.shouldBeNull()
            }

            "Malformed URL returns null" {

                val result = CloudinaryImage.fromUrl("not-a-url-at-all")

                result.shouldBeNull()
            }
        }

        "url property" - {

            "Reconstructs URL correctly (round-trip)" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/folder/image.jpg"

                val result = CloudinaryImage.fromUrl(url)

                result.shouldNotBeNull()
                result.url shouldBe url
            }

            "Reconstructs URL with quality keywords (round-trip)" {

                val url = "https://res.cloudinary.com/myapp/image/upload/dpr_auto,f_auto/v1234/image.jpg"

                val result = CloudinaryImage.fromUrl(url)

                result.shouldNotBeNull()
                result.url shouldBe url
            }
        }

        "autoDpr" - {

            "Adds dpr_auto to quality" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val image = CloudinaryImage.fromUrl(url)!!

                val result = image.autoDpr()

                result.quality shouldBe mapOf("dpr_auto" to null)
            }
        }

        "autoFormat" - {

            "Adds f_auto to quality" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val image = CloudinaryImage.fromUrl(url)!!

                val result = image.autoFormat()

                result.quality shouldBe mapOf("f_auto" to null)
            }
        }

        "autoQuality" - {

            "Adds q_auto to quality" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val image = CloudinaryImage.fromUrl(url)!!

                val result = image.autoQuality()

                result.quality shouldBe mapOf("q_auto" to null)
            }
        }

        "transformWidth" - {

            "Adds w_400 to transform" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val image = CloudinaryImage.fromUrl(url)!!

                val result = image.transformWidth(400)

                result.transform shouldBe mapOf("w_400" to null)
            }
        }

        "Chaining" - {

            "autoDpr, autoFormat, autoQuality has all three" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val image = CloudinaryImage.fromUrl(url)!!

                val result = image.autoDpr().autoFormat().autoQuality()

                result.quality shouldBe mapOf(
                    "dpr_auto" to null,
                    "f_auto" to null,
                    "q_auto" to null,
                )
            }

            "transformWidth after quality preserves both" {

                val url = "https://res.cloudinary.com/myapp/image/upload/v1234/image.jpg"

                val image = CloudinaryImage.fromUrl(url)!!

                val result = image.autoFormat().autoQuality().transformWidth(800)

                result.quality shouldBe mapOf("f_auto" to null, "q_auto" to null)
                result.transform shouldBe mapOf("w_800" to null)
                result.url shouldBe "https://res.cloudinary.com/myapp/image/upload/f_auto,q_auto/w_800/v1234/image.jpg"
            }
        }
    }
}
