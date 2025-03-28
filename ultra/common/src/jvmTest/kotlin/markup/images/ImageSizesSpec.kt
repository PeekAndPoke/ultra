package de.peekandpoke.ultra.common.markup.images

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ImageSizesSpec : FreeSpec() {
    init {
        "Predefined ImageSizes" - {

            "ImageSizes.mobile100desktop25" {

                val subject = ImageSizes.mobile100desktop25

                subject.defaultSize shouldBe ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)

                subject.sizes shouldBe listOf(
                    ImageSizes.Entry.MaxWidth(
                        px = 320, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 512, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 640, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1024, ImageSizes.CssSize(25.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1440, ImageSizes.CssSize(25.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 2048, ImageSizes.CssSize(25.0f, ImageSizes.CssUnit.VW)
                    ),
                )
            }

            "ImageSizes.mobile100desktop33" {

                val subject = ImageSizes.mobile100desktop33

                subject.defaultSize shouldBe ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)

                subject.sizes shouldBe listOf(
                    ImageSizes.Entry.MaxWidth(
                        px = 320, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 512, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 640, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1024, ImageSizes.CssSize(33.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1440, ImageSizes.CssSize(33.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 2048, ImageSizes.CssSize(33.0f, ImageSizes.CssUnit.VW)
                    ),
                )
            }

            "ImageSizes.mobile100desktop50" {

                val subject = ImageSizes.mobile100desktop50

                subject.defaultSize shouldBe ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)

                subject.sizes shouldBe listOf(
                    ImageSizes.Entry.MaxWidth(
                        px = 320, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 512, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 640, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1024, ImageSizes.CssSize(50.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1440, ImageSizes.CssSize(50.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 2048, ImageSizes.CssSize(50.0f, ImageSizes.CssUnit.VW)
                    ),
                )
            }

            "ImageSizes.mobile100desktop6" {

                val subject = ImageSizes.mobile100desktop66

                subject.defaultSize shouldBe ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)

                subject.sizes shouldBe listOf(
                    ImageSizes.Entry.MaxWidth(
                        px = 320, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 512, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 640, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1024, ImageSizes.CssSize(66.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1440, ImageSizes.CssSize(66.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 2048, ImageSizes.CssSize(66.0f, ImageSizes.CssUnit.VW)
                    ),
                )
            }

            "ImageSizes.mobile100desktop75" {

                val subject = ImageSizes.mobile100desktop75

                subject.defaultSize shouldBe ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)

                subject.sizes shouldBe listOf(
                    ImageSizes.Entry.MaxWidth(
                        px = 320, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 512, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 640, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1024, ImageSizes.CssSize(75.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1440, ImageSizes.CssSize(75.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 2048, ImageSizes.CssSize(75.0f, ImageSizes.CssUnit.VW)
                    ),
                )
            }

            "ImageSizes.mobile100desktop100" {

                val subject = ImageSizes.mobile100desktop100

                subject.defaultSize shouldBe ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)

                subject.sizes shouldBe listOf(
                    ImageSizes.Entry.MaxWidth(
                        px = 320, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 512, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 640, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1024, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 1440, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                    ImageSizes.Entry.MaxWidth(
                        px = 2048, ImageSizes.CssSize(100.0f, ImageSizes.CssUnit.VW)
                    ),
                )
            }
        }
    }
}
