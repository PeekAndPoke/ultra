@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.image

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.fixture.LoremIpsum
import generated.ExtractedCodeBlocks
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.ImagePage() = comp {
    ImagePage(it)
}

class ImagePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Image")

        ui.basic.segment {
            ui.dividing.header H1 { +"Image" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/image.html")

            ui.dividing.header H2 { +"Types" }

            renderBasicImage()
            renderLinkImage()

            ui.dividing.header H2 { +"States" }

            renderHiddenImage()
            renderDisabledImage()

            ui.dividing.header H2 { +"Variations" }

            renderAvatarImage()
            renderBorderedImage()
            renderFluidImage()
            renderRoundedImage()
            renderCircularImage()
            renderVerticalAlignmentImage()
            renderCenteredImage()
            renderSpacedImage()
            renderFloatedImage()

            ui.dividing.header H2 { +"Groups" }

            renderSizedGroupsOfImages()
        }
    }

    private fun FlowContent.renderBasicImage() = example {
        ui.header H3 { +"Basic image" }

        ui.message {
            +"Unless a size is specified, images will use the original dimensions of the image up "
            +"to the size of its container.. "
        }

        ui.stackable.three.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderBasicImage_1,
                ) {
                    // <CodeBlock renderBasicImage_1>
                    ui.image Img { src = "images/wireframe/image.png" }
                    // </CodeBlock>
                }
            }
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderBasicImage_2,
                ) {
                    // <CodeBlock renderBasicImage_2>
                    ui.small.image Img { src = "images/wireframe/image.png" }
                    // </CodeBlock>
                }
            }
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderBasicImage_3,
                ) {
                    // <CodeBlock renderBasicImage_3>
                    ui.large.image Img { src = "images/wireframe/image.png" }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderLinkImage() = example {
        ui.header H3 { +"Link image" }

        p { +"An image can be formatted to link to other content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderLinkImage,
        ) {
            // <CodeBlock renderLinkImage>
            ui.medium.image A {
                href = "https://example.com"
                img { src = "images/wireframe/image.png" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHiddenImage() = example {
        ui.header H3 { +"Hidden" }

        p { +"An image can be hidden." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderHiddenImage,
        ) {
            // <CodeBlock renderHiddenImage>
            ui.hidden.image Img {
                src = "images/wireframe/image.png"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabledImage() = example {
        ui.header H3 { +"Disabled" }

        p { +"An image can be disabled." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderDisabledImage,
        ) {
            // <CodeBlock renderDisabledImage>
            ui.disabled.medium.image Img {
                src = "images/wireframe/image.png"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAvatarImage() = example {
        ui.header H3 { +"Avatar" }

        p { +"An image may be formatted to appear inline with text as an avatar." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderAvatarImage,
        ) {
            // <CodeBlock renderAvatarImage>
            ui.avatar.image Img {
                src = "images/wireframe/image.png"
            }
            span { +"Username" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBorderedImage() = example {
        ui.header H3 { +"Bordered" }

        p { +"An image may include a border to emphasize the edges of white or transparent content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderBorderedImage,
        ) {
            // <CodeBlock renderBorderedImage>
            ui.medium.bordered.image Img {
                src = "images/wireframe/white-image.png"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFluidImage() = example {
        ui.header H3 { +"Fluid" }

        p { +"An image can take up the width of its container." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderFluidImage,
        ) {
            // <CodeBlock renderFluidImage>
            ui.segment {
                ui.fluid.image Img {
                    src = "images/avatar2/large/eve.png"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRoundedImage() = example {
        ui.header H3 { +"Rounded" }

        p { +"An image may appear rounded." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderRoundedImage,
        ) {
            // <CodeBlock renderRoundedImage>
            ui.medium.rounded.image Img {
                src = "images/wireframe/image.png"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCircularImage() = example {
        ui.header H3 { +"Circular" }

        p { +"An image may appear circular." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderCircularImage,
        ) {
            // <CodeBlock renderCircularImage>
            ui.medium.circular.image Img {
                src = "images/avatar2/large/elliot.jpg"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVerticalAlignmentImage() = example {
        ui.header H3 { +"Vertical Alignment" }

        p { +"An image can specify its vertical alignment." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderVerticalAlignmentImage,
        ) {
            // <CodeBlock renderVerticalAlignmentImage>
            ui.segment {
                ui.top.aligned.tiny.right.spaced.image Img { src = "images/wireframe/image.png" }
                span { +"Top aligned" }

                ui.divider {}

                ui.middle.aligned.tiny.right.spaced.image Img { src = "images/wireframe/image.png" }
                span { +"Top aligned" }

                ui.divider {}

                ui.bottom.aligned.tiny.right.spaced.image Img { src = "images/wireframe/image.png" }
                span { +"Top aligned" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCenteredImage() = example {
        ui.header H3 { +"Centered" }

        p { +"An image can appear centered in a content block" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderCenteredImage,
        ) {
            // <CodeBlock renderCenteredImage>
            ui.segment {
                ui.medium.centered.image Img { src = "images/wireframe/image.png" }
                +LoremIpsum.words(50)
                ui.small.centered.image Img { src = "images/wireframe/image.png" }
                +LoremIpsum.words(50)
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSpacedImage() = example {
        ui.header H3 { +"Spaced" }

        p { +"An image can specify that it needs an additional spacing to separate it from nearby content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderSpacedImage_1,
        ) {
            // <CodeBlock renderSpacedImage_1>
            ui.segment {
                ui.mini.right.spaced.image Img { src = "images/wireframe/image.png" }
                +LoremIpsum.words(10)
                ui.mini.spaced.image Img { src = "images/wireframe/image.png" }
                +LoremIpsum.words(10)
                ui.mini.left.spaced.image Img { src = "images/wireframe/image.png" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloatedImage() = example {
        ui.header H3 { +"Floated" }

        p { +"An image can sit to the left or right of other content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderFloatedImage,
        ) {
            // <CodeBlock renderFloatedImage>
            ui.segment {
                ui.small.left.floated.image Img { src = "images/wireframe/image.png" }

                p { +LoremIpsum.words(35) }

                ui.small.right.floated.image Img { src = "images/wireframe/image.png" }

                p { +LoremIpsum.words(35) }

                p { +LoremIpsum.words(35) }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSizedGroupsOfImages() = example {
        ui.header H3 { +"Size" }

        p { +"A group of images can be formatted to have the same size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderSizedGroupsOfImages_1,
        ) {
            // <CodeBlock renderSizedGroupsOfImages_1>
            ui.tiny.images {
                ui.images Img { src = "images/avatar2/large/elliot.jpg" }
                ui.images Img { src = "images/avatar2/large/eve.png" }
                ui.images Img { src = "images/avatar2/large/elyse.png" }
                ui.images Img { src = "images/avatar2/large/molly.png" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_image_ImagePage_kt_renderSizedGroupsOfImages_2,
        ) {
            // <CodeBlock renderSizedGroupsOfImages_2>
            ui.small.images {
                ui.images Img { src = "images/avatar2/large/elliot.jpg" }
                ui.images Img { src = "images/avatar2/large/eve.png" }
                ui.images Img { src = "images/avatar2/large/elyse.png" }
                ui.images Img { src = "images/avatar2/large/molly.png" }
            }
            // </CodeBlock>
        }
    }
}
