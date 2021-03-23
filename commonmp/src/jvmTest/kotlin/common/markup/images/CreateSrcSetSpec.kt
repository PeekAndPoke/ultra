package de.peekandpoke.ultra.common.markup.images

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class CreateSrcSetSpec : FreeSpec() {

    init {
        "Undetected image url" {

            val result = createSrcSet("http://www.example.com/image.jpg", ImageSizes.mobile100desktop50)

            result.shouldBeNull()
        }

        "Cloudinary" - {

            "Cloudinary image must be detected correctly" {

                val url = "https://res.cloudinary.com/jointhebase/image/upload/v1615973279/image.jpg"

                val result = createSrcSet(url, ImageSizes.default)

                result shouldBe ImageSrcSet(
                    url = url,
                    entries = listOf(
                        ImageSrcSet.Entry(
                            maxWidth = 320,
                            url = "https://res.cloudinary.com/jointhebase/image/upload/f_auto,dpr_auto,q_auto/w_320/v1615973279/image.jpg"
                        ),
                        ImageSrcSet.Entry(
                            maxWidth = 512,
                            url = "https://res.cloudinary.com/jointhebase/image/upload/f_auto,dpr_auto,q_auto/w_512/v1615973279/image.jpg"
                        ),
                        ImageSrcSet.Entry(
                            maxWidth = 640,
                            url = "https://res.cloudinary.com/jointhebase/image/upload/f_auto,dpr_auto,q_auto/w_640/v1615973279/image.jpg"
                        ),
                        ImageSrcSet.Entry(
                            maxWidth = 1024,
                            url = "https://res.cloudinary.com/jointhebase/image/upload/f_auto,dpr_auto,q_auto/w_1024/v1615973279/image.jpg"
                        ),
                        ImageSrcSet.Entry(
                            maxWidth = 1440,
                            url = "https://res.cloudinary.com/jointhebase/image/upload/f_auto,dpr_auto,q_auto/w_1440/v1615973279/image.jpg"
                        ),
                        ImageSrcSet.Entry(
                            maxWidth = 2048,
                            url = "https://res.cloudinary.com/jointhebase/image/upload/f_auto,dpr_auto,q_auto/w_2048/v1615973279/image.jpg"
                        ),
                    )
                )
            }
        }
    }
}
