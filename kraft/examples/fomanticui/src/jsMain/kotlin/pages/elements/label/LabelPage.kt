@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.label

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.fixture.LoremIpsum
import generated.ExtractedCodeBlocks
import kotlinx.css.Overflow
import kotlinx.css.overflow
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.LabelPage() = comp {
    LabelPage(it)
}

class LabelPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Label")

        ui.basic.segment {
            ui.dividing.header H1 { +"Label" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/label.html")

            ui.dividing.header H2 { +"Types" }

            renderLabel()
            renderImageLabel()
            renderPointingLabel()
            renderCornerLabel()
            renderTagLabel()
            renderRibbonLabel()
            renderAttachedLabel()
            renderHorizontalLabel()
            renderFloatingLabel()

            ui.dividing.header H2 { +"Content" }

            renderDetailContentLabel()
            renderIconContentLabel()
            renderImageContentLabel()
            renderLinkContentLabel()

            ui.dividing.header H2 { +"Variations" }

            renderCircularLabel()
            renderBasicLabel()
            renderColoredLabel()
            renderSizedLabel()

            ui.dividing.header H2 { +"Groups" }

            renderGroupSize()
            renderColoredGroup()
            renderBasicGroup()
            renderTagGroup()
            renderCircularGroup()

            ui.dividing.header H2 { +"Inverted" }

            renderInvertedVariants()
        }
    }

    private fun FlowContent.renderLabel() = example {
        ui.header H3 { +"Label" }

        p { +"A label" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderLabel,
        ) {
            // <CodeBlock renderLabel>
            ui.label {
                icon.mail()
                +"23"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderImageLabel() = example {
        ui.header H3 { +"Image" }

        p { +"A label can be formatted to emphasize an image" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderImageLabel_1,
        ) {
            // <CodeBlock renderImageLabel_1>
            ui.image.label {
                img { src = "images/avatar2/large/elyse.png" }
                +"Elyse"
            }
            ui.image.label {
                img { src = "images/avatar2/large/elliot.jpg" }
                +"Elliot"
            }
            ui.image.label {
                img { src = "images/avatar2/large/molly.png" }
                +"Molly"
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderImageLabel_2,
        ) {
            // <CodeBlock renderImageLabel_2>
            ui.blue.image.label A {
                img { src = "images/avatar2/large/elyse.png" }
                +"Elyse"
                noui.detail { +"Friend" }
            }
            ui.green.image.label A {
                img { src = "images/avatar2/large/elliot.jpg" }
                +"Elliot"
                noui.detail { +"Student" }
            }
            ui.orange.image.label A {
                img { src = "images/avatar2/large/molly.png" }
                +"Molly"
                noui.detail { +"Co-worker" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderImageLabel_3,
        ) {
            // <CodeBlock renderImageLabel_3>
            ui.image.label {
                img { src = "images/avatar2/large/elyse.png" }
                +"Elyse"
                icon.delete()
            }
            ui.image.label {
                img { src = "images/avatar2/large/elliot.jpg" }
                +"Elliot"
                icon.green.delete()
            }
            ui.image.label {
                img { src = "images/avatar2/large/molly.png" }
                +"Molly"
                icon.red.delete()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPointingLabel() = example {
        ui.header H3 { +"Pointing" }

        p { +"A label can point to content next to it" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderPointingLabel_1,
        ) {
            // <CodeBlock renderPointingLabel_1>
            ui.fluid.form {
                noui.field {
                    input { type = InputType.text; placeholder = "First name" }
                    ui.pointing.label {
                        +"Please enter a value"
                    }
                }

                ui.divider()

                noui.field {
                    ui.pointing.below.label {
                        +"Please enter a value"
                    }
                    input { type = InputType.text; placeholder = "Last name" }
                }

                ui.divider()

                noui.inline.field {
                    input { type = InputType.text; placeholder = "Username" }
                    ui.left.pointing.blue.label {
                        +"Please enter a value"
                    }
                }

                ui.divider()

                noui.inline.field {
                    ui.right.pointing.red.label {
                        +"Please enter a value"
                    }
                    input { type = InputType.text; placeholder = "Password" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCornerLabel() = example {
        ui.header H3 { +"Corner" }

        p { +"A label can position itself in the corner of an element" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderCornerLabel_1,
        ) {
            // <CodeBlock renderCornerLabel_1>
            ui.two.column.grid {
                ui.column {
                    ui.fluid.image {
                        ui.left.corner.label { icon.heart() }
                        img { src = "images/wireframe/image.png" }
                    }
                }
                ui.column {
                    ui.fluid.image {
                        ui.right.corner.red.label { icon.star() }
                        img { src = "images/wireframe/image.png" }
                    }
                }
            }
            // </CodeBlock>
        }

        ui.yellow.message {
            +"If the container is "
            ui.label { +"rounded" }
            +" you will need to add "
            ui.label { +"overflow: hidden" }
            +" to produce a rounded label"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderCornerLabel_2,
        ) {
            // <CodeBlock renderCornerLabel_2>
            ui.two.column.grid {
                ui.column {
                    ui.fluid.rounded.image {
                        css {
                            overflow = Overflow.hidden
                        }
                        ui.left.corner.label { icon.heart() }
                        img { src = "images/wireframe/image.png" }
                    }
                }
                ui.column {
                    ui.fluid.rounded.image {
                        css {
                            overflow = Overflow.hidden
                        }
                        ui.right.corner.red.label { icon.star() }
                        img { src = "images/wireframe/image.png" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTagLabel() = example {
        ui.header H3 { +"Tag" }

        p { +"A label can appear as a tag" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderTagLabel_1,
        ) {
            // <CodeBlock renderTagLabel_1>
            ui.tag.label A { +"New" }
            ui.red.tag.label A { +"Upcoming" }
            ui.teal.tag.label A { +"Featured" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRibbonLabel() = example {
        ui.header H3 { +"Ribbon" }

        p { +"A label can appear as a ribbon attaching itself to an element." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderRibbonLabel_1,
        ) {
            ui.basic.segment {
                // <CodeBlock renderRibbonLabel_1>
                ui.raised.segment {
                    ui.red.ribbon.label { +"Overview" }
                    span { +"Account details" }
                    p { +LoremIpsum(30) }
                }
                ui.raised.segment {
                    ui.blue.ribbon.label { icon.users(); +"Community" }
                    span { +"User comments" }
                    p { +LoremIpsum(30) }
                }
                ui.raised.segment {
                    ui.green.ribbon.label { icon.cogs() }
                    span { +"Settings" }
                    p { +LoremIpsum(30) }
                }
                // </CodeBlock>
            }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderRibbonLabel_2,
        ) {
            ui.basic.segment {
                // <CodeBlock renderRibbonLabel_2>
                ui.raised.segment {
                    ui.red.right.ribbon.label { +"Overview" }
                    span { +"Account details" }
                    p { +LoremIpsum(30) }
                }
                ui.raised.segment {
                    ui.blue.right.ribbon.label { icon.users(); +"Community" }
                    span { +"User comments" }
                    p { +LoremIpsum(30) }
                }
                ui.raised.segment {
                    ui.green.right.ribbon.label { icon.cogs() }
                    span { +"Settings" }
                    p { +LoremIpsum(30) }
                }
                // </CodeBlock>
            }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderRibbonLabel_3,
        ) {
            ui.basic.segment {                // <CodeBlock renderRibbonLabel_3>
                ui.two.column.grid {
                    ui.column {
                        ui.fluid.image {
                            ui.black.ribbon.label { icon.hotel(); +"Hotel" }
                            img { src = "images/wireframe/image.png" }
                        }
                    }
                    ui.column {
                        ui.fluid.bordered.image {
                            ui.blue.right.ribbon.label { icon.utensil_spoon(); +"Food" }
                            img { src = "images/wireframe/white-image.png" }
                        }
                    }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderAttachedLabel() = example {
        ui.header H3 { +"Attached" }

        p { +"A label can attach to a content segment" }

        ui.yellow.message {
            p {
                +"Attached labels attempt to intelligently pad other content to account for their position, "
                +"but may not in all cases apply this padding correctly."
            }
            p {
                +"If this happens you may need to manually correct the padding of the other elements "
                +"inside the container."
            }
        }

        ui.two.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderAttachedLabel_1,
                ) {
                    // <CodeBlock renderAttachedLabel_1>
                    ui.segment {
                        ui.top.attached.label { +"top attached" }
                        p {
                            +LoremIpsum(30)
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderAttachedLabel_2,
                ) {
                    // <CodeBlock renderAttachedLabel_2>
                    ui.segment {
                        ui.bottom.attached.label { +"bottom attached" }
                        p {
                            +LoremIpsum(30)
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderAttachedLabel_3,
                ) {
                    // <CodeBlock renderAttachedLabel_3>
                    ui.segment {
                        ui.top.right.attached.label { +"top right attached" }
                        p {
                            +LoremIpsum(30)
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderAttachedLabel_4,
                ) {
                    // <CodeBlock renderAttachedLabel_4>
                    ui.segment {
                        ui.top.left.attached.label { +"top left attached" }
                        p {
                            +LoremIpsum(30)
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderAttachedLabel_5,
                ) {
                    // <CodeBlock renderAttachedLabel_5>
                    ui.segment {
                        ui.bottom.right.attached.label { +"bottom right attached" }
                        p {
                            +LoremIpsum(30)
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderAttachedLabel_6,
                ) {
                    // <CodeBlock renderAttachedLabel_6>
                    ui.segment {
                        ui.bottom.left.attached.label { +"bottom left attached" }
                        p {
                            +LoremIpsum(30)
                        }
                    }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderHorizontalLabel() = example {
        ui.header H3 { +"Horizontal" }

        p { +"A horizontal label is formatted to label content along-side it horizontally." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderHorizontalLabel_1,
        ) {
            // <CodeBlock renderHorizontalLabel_1>
            ui.divided.selection.list {
                noui.item A {
                    ui.red.horizontal.label { +"Fruit" }
                    +"Kumquats"
                }
                noui.item A {
                    ui.pink.horizontal.label { +"Candy" }
                    +"Ice cream"
                }
                noui.item A {
                    ui.orange.horizontal.label { +"Orange" }
                    +"Orange"
                }
                noui.item A {
                    ui.horizontal.label { +"Dog" }
                    +"Poodle"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloatingLabel() = example {
        ui.header H3 { +"Floating" }

        p { +"A label can float above or below another element." }

        ui.yellow.message {
            +"A floating label must be positioned inside a container with"
            ui.horizontal.label { +"position: relative" }
            +"to display properly."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderFloatingLabel_1,
        ) {
            // <CodeBlock renderFloatingLabel_1>
            ui.compact.menu {
                noui.item A {
                    +"Message"
                    ui.red.floating.label { +"23" }
                }
                noui.item A {
                    +"Friend"
                    ui.pink.floating.label { +"22" }
                }
            }
            // </CodeBlock>
        }

        ui.info.message { +"Label can be floating to the left" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderFloatingLabel_2,
        ) {
            // <CodeBlock renderFloatingLabel_2>
            ui.compact.menu {
                noui.item A {
                    icon.mail()
                    +"Message"
                    ui.red.left.floating.label { +"23 Mails" }
                }
                noui.item A {
                    icon.users()
                    +"Friend"
                    ui.pink.left.floating.label { +"22 Friends" }
                }
            }
            // </CodeBlock>
        }

        ui.info.message { +"Label can be floating at the bottom" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderFloatingLabel_3,
        ) {
            // <CodeBlock renderFloatingLabel_3>
            ui.compact.menu {
                noui.item A {
                    icon.mail()
                    +"Message"
                    ui.red.bottom.floating.label { +"23 Mails" }
                }
                noui.item A {
                    icon.users()
                    +"Friend"
                    ui.pink.bottom.floating.label { +"22 Friends" }
                }
            }
            // </CodeBlock>
        }

        ui.info.message { +"Label can be floating at the bottom left" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderFloatingLabel_4,
        ) {
            // <CodeBlock renderFloatingLabel_4>
            ui.compact.menu {
                noui.item A {
                    icon.mail()
                    +"Message"
                    ui.red.bottom.left.floating.label { +"23 Mails" }
                }
                noui.item A {
                    icon.users()
                    +"Friend"
                    ui.pink.bottom.left.floating.label { +"22 Friends" }
                }
            }
            // </CodeBlock>
        }

        ui.info.message { +"Floating Labels containing large text can be aligned to the left or right" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderFloatingLabel_5,
        ) {
            // <CodeBlock renderFloatingLabel_5>
            ui.compact.menu {
                noui.item A {
                    icon.mail()
                    +"Messages from KRAFT users"
                    ui.left.aligned.floating.red.label { +"23 Mails unread" }
                }
                noui.item A {
                    icon.users()
                    +"Friends of the KRAFT community"
                    ui.right.aligned.floating.pink.label { +"22 Friends online" }
                }
            }
            // </CodeBlock>
        }

        ui.info.message { +"Floating Labels containing large text can be aligned to the bottom left or right" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderFloatingLabel_6,
        ) {
            // <CodeBlock renderFloatingLabel_6>
            ui.compact.menu {
                noui.item A {
                    icon.mail()
                    +"Messages from KRAFT users"
                    ui.bottom.left.aligned.floating.red.label { +"23 Mails unread" }
                }
                noui.item A {
                    icon.users()
                    +"Friends of the KRAFT community"
                    ui.bottom.right.aligned.floating.pink.label { +"22 Friends online" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDetailContentLabel() = example {
        ui.header H3 { +"Detail" }

        p { +"A label can contain a detail" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderDetailContentLabel,
        ) {
            // <CodeBlock renderDetailContentLabel>
            ui.label {
                +"Dogs"
                noui.detail { +"23" }
            }

            ui.green.label {
                +"Trees"
                noui.detail { icon.tree() }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIconContentLabel() = example {
        ui.header H3 { +"Icon" }

        p { +"A label can include an icon" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderIconContentLabel,
        ) {
            // <CodeBlock renderIconContentLabel>
            ui.label { icon.mail(); +"Mail" }
            ui.red.label { icon.exclamation_triangle(); +"Error" }
            ui.brown.label { icon.dog(); +"Dog" }
            ui.black.label { icon.cat(); +"Cat" }
            // </CodeBlock>
        }

        ui.info.message { +"Icons can be placed to the right inside a label." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderIconContentLabel_2,
        ) {
            // <CodeBlock renderIconContentLabel_2>
            ui.right.icon.label { +"Mail"; icon.mail() }
            ui.right.icon.red.label { +"Error"; icon.exclamation_triangle() }
            ui.right.icon.brown.label { +"Dog"; icon.dog() }
            ui.right.icon.black.label { +"Cat"; icon.cat() }
            // </CodeBlock>
        }

        ui.info.message { +"Labels can contain individual icons without text." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderIconContentLabel_3,
        ) {
            // <CodeBlock renderIconContentLabel_3>
            ui.icon.label { icon.mail() }
            ui.icon.red.label { icon.exclamation_triangle() }
            ui.icon.brown.label { icon.dog() }
            ui.icon.black.label { icon.cat() }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderImageContentLabel() = example {
        ui.header H3 { +"Image" }

        p { +"A label can include an image" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderImageContentLabel,
        ) {
            // <CodeBlock renderImageContentLabel>
            ui.label A {
                ui.right.spaced.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                +"Elliot"
            }
            ui.basic.green.label A {
                ui.right.spaced.rounded.image Img { src = "images/avatar2/large/eve.png" }
                +"Eve"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLinkContentLabel() = example {
        ui.header H3 { +"Link" }

        p { +"A label can be a link or contain an item that links" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderLinkContentLabel,
        ) {
            // <CodeBlock renderLinkContentLabel>
            ui.label A {
                icon.mail()
                +"23"
            }

            ui.label {
                icon.mail()
                +"23"
                noui.detail A {
                    href = "https://example.com"
                    +"View"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCircularLabel() = example {
        ui.header H3 { +"Circular" }

        p { +"A label can be circular" }

        ui.stackable.three.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderCircularLabel,
                ) {
                    // <CodeBlock renderCircularLabel>
                    ui.circular.red.label { +"1" }
                    ui.circular.orange.label { +"2" }
                    ui.circular.yellow.label { +"3" }
                    ui.circular.olive.label { +"4" }
                    ui.circular.green.label { +"5" }
                    ui.circular.teal.label { +"6" }
                    ui.circular.blue.label { +"7" }
                    ui.circular.violet.label { +"8" }
                    ui.circular.purple.label { +"9" }
                    ui.circular.pink.label { +"10" }
                    ui.circular.brown.label { +"11" }
                    ui.circular.grey.label { +"12" }
                    ui.circular.black.label { +"13" }
                    ui.circular.white.label { +"14" }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderCircularLabel_2,
                ) {
                    // <CodeBlock renderCircularLabel_2>
                    ui.circular.red.icon.label { icon.mail() }
                    ui.circular.orange.icon.label { icon.mail() }
                    ui.circular.yellow.icon.label { icon.mail() }
                    ui.circular.olive.icon.label { icon.mail() }
                    ui.circular.green.icon.label { icon.mail() }
                    ui.circular.teal.icon.label { icon.mail() }
                    ui.circular.blue.icon.label { icon.mail() }
                    ui.circular.violet.icon.label { icon.mail() }
                    ui.circular.purple.icon.label { icon.mail() }
                    ui.circular.pink.icon.label { icon.mail() }
                    ui.circular.brown.icon.label { icon.mail() }
                    ui.circular.grey.icon.label { icon.mail() }
                    ui.circular.black.icon.label { icon.mail() }
                    ui.circular.white.icon.label { icon.mail() }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderCircularLabel_3,
                ) {
                    // <CodeBlock renderCircularLabel_3>
                    ui.circular.empty.red.label { }
                    ui.circular.empty.orange.label { }
                    ui.circular.empty.yellow.label { }
                    ui.circular.empty.olive.label { }
                    ui.circular.empty.green.label { }
                    ui.circular.empty.teal.label { }
                    ui.circular.empty.blue.label { }
                    ui.circular.empty.violet.label { }
                    ui.circular.empty.purple.label { }
                    ui.circular.empty.pink.label { }
                    ui.circular.empty.brown.label { }
                    ui.circular.empty.grey.label { }
                    ui.circular.empty.black.label { }
                    ui.circular.empty.white.label { }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderBasicLabel() = example {
        ui.header H3 { +"Basic" }

        p { +"A label can be basic" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderBasicLabel,
        ) {
            // <CodeBlock renderBasicLabel>
            ui.basic.label { +"Basic" }
            ui.basic.pointing.label { +"Pointing" }
            ui.basic.image.label {
                img(src = "images/avatar2/large/elliot.jpg")
                +"Elliot"
            }
            ui.basic.red.label { +"Red" }
            ui.basic.blue.label { +"Blue" }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderBasicLabel_2,
        ) {
            // <CodeBlock renderBasicLabel_2>
            ui.basic.tag.label { +"Basic" }
            ui.basic.tag.red.label { +"Red" }
            ui.basic.tag.blue.label { +"Blue" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColoredLabel() = example {
        ui.header H3 { +"Colored" }

        p { +"A label can have different colors" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderColoredLabel,
        ) {
            // <CodeBlock renderColoredLabel>
            ui.primary.label { +"primary" }
            ui.secondary.label { +"secondary" }
            ui.red.label { +"red" }
            ui.orange.label { +"orange" }
            ui.yellow.label { +"yellow" }
            ui.olive.label { +"olive" }
            ui.green.label { +"green" }
            ui.teal.label { +"teal" }
            ui.blue.label { +"blue" }
            ui.violet.label { +"violet" }
            ui.purple.label { +"purple" }
            ui.pink.label { +"pink" }
            ui.brown.label { +"brown" }
            ui.grey.label { +"grey" }
            ui.black.label { +"black" }
            ui.white.label { +"white" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSizedLabel() = example {
        ui.header H3 { +"Size" }

        p { +"A label can be small or large" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderSizedLabel,
        ) {
            // <CodeBlock renderSizedLabel>
            ui.mini.label { +"mini" }
            ui.tiny.label { +"tiny" }
            ui.small.label { +"small" }
            ui.medium.label { +"medium" }
            ui.large.label { +"large" }
            ui.huge.label { +"huge" }
            ui.massive.label { +"massive" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderGroupSize() = example {
        ui.header H3 { +"Group Size" }

        p { +"Labels can share sizes together" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderGroupSize,
        ) {
            // <CodeBlock renderGroupSize>
            ui.huge.labels {
                ui.label { +"Fun" }
                ui.label { +"Happy" }
                ui.label { +"Smart" }
                ui.label { +"Witty" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColoredGroup() = example {
        ui.header H3 { +"Colored Group" }

        p { +"Labels can share colors together" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderColoredGroup,
        ) {
            // <CodeBlock renderColoredGroup>
            ui.blue.labels {
                ui.label { +"Fun" }
                ui.label { +"Happy" }
                ui.label { +"Smart" }
                ui.label { +"Witty" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBasicGroup() = example {
        ui.header H3 { +"Basic Group" }

        p { +"Labels can share their style together" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderBasicGroup,
        ) {
            // <CodeBlock renderBasicGroup>
            ui.basic.labels {
                ui.label { +"Fun" }
                ui.label { +"Happy" }
                ui.label { +"Smart" }
                ui.label { +"Witty" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTagGroup() = example {
        ui.header H3 { +"Tag Group" }

        p { +"Labels can share tag formatting" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderTagGroup,
        ) {
            // <CodeBlock renderTagGroup>
            ui.tag.labels {
                ui.label { +"Fun" }
                ui.label { +"Happy" }
                ui.label { +"Smart" }
                ui.label { +"Witty" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCircularGroup() = example {
        ui.header H3 { +"Circular Group" }

        p { +"Labels can share shapes" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderCircularGroup,
        ) {
            // <CodeBlock renderCircularGroup>
            ui.circular.labels {
                ui.label { +"Fun" }
                ui.label { +"Happy" }
                ui.label { +"Smart" }
                ui.label { +"Witty" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInvertedVariants() = example {
        ui.header H3 { +"All variants can be inverted" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_label_LabelPage_kt_renderInvertedVariants,
        ) {
            // <CodeBlock renderInvertedVariants>
            ui.inverted.segment {
                ui.primary.inverted.label { +"primary" }
                ui.secondary.inverted.label { +"secondary" }
                ui.red.inverted.label { +"red" }
                ui.orange.inverted.label { +"orange" }
                ui.yellow.inverted.label { +"yellow" }
                ui.olive.inverted.label { +"olive" }
                ui.green.inverted.label { +"green" }
                ui.violet.inverted.label { +"violet" }
                ui.purple.inverted.label { +"purple" }
                ui.brown.inverted.label { +"brown" }
                ui.black.inverted.label { +"black" }
                ui.white.inverted.label { +"white" }

                ui.hidden.divider()

                ui.basic.primary.inverted.label { +"primary" }
                ui.basic.secondary.inverted.label { +"secondary" }
                ui.basic.red.inverted.label { +"red" }
                ui.basic.orange.inverted.label { +"orange" }
                ui.basic.yellow.inverted.label { +"yellow" }
                ui.basic.olive.inverted.label { +"olive" }
                ui.basic.green.inverted.label { +"green" }
                ui.basic.violet.inverted.label { +"violet" }
                ui.basic.purple.inverted.label { +"purple" }
                ui.basic.brown.inverted.label { +"brown" }
                ui.basic.black.inverted.label { +"black" }
                ui.basic.white.inverted.label { +"white" }

                ui.hidden.divider()

                ui.primary.inverted.tag.label { +"primary" }
                ui.secondary.inverted.tag.label { +"secondary" }
                ui.red.inverted.tag.label { +"red" }
                ui.orange.inverted.tag.label { +"orange" }
                ui.yellow.inverted.tag.label { +"yellow" }
                ui.olive.inverted.tag.label { +"olive" }
                ui.green.inverted.tag.label { +"green" }
                ui.violet.inverted.tag.label { +"violet" }
                ui.purple.inverted.tag.label { +"purple" }
                ui.brown.inverted.tag.label { +"brown" }
                ui.black.inverted.tag.label { +"black" }
                ui.white.inverted.tag.label { +"white" }
            }
            // </CodeBlock>
        }
    }
}
