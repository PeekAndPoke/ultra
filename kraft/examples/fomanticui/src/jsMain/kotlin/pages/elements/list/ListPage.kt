@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.list

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.allSizes
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.fixture.LoremIpsum
import generated.ExtractedCodeBlocks
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.ListPage() = comp {
    ListPage(it)
}

class ListPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | List")

        ui.basic.segment {
            ui.dividing.header H1 { +"List" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/list.html")

            ui.dividing.header H2 { +"Types" }

            renderList()
            renderBulletedList()
            renderOrderedList()
            renderLinkList()

            ui.dividing.header H2 { +"Content" }

            renderImageContent()
            renderHeaderContent()
            renderDescriptionContent()

            ui.dividing.header H2 { +"Variations" }

            renderHorizontalList()
            renderInvertedList()
            renderSelectionList()
            renderAnimatedList()
            renderRelaxedList()
            renderDividedList()
            renderCelledList()
            renderSizedList()

            ui.dividing.header H2 { +"Content Variations" }

            renderVerticalAlignment()
            renderFloatedContent()
        }
    }

    private fun FlowContent.renderList() = example {
        ui.header H3 { +"List" }

        p { +"A list groups related content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderList_1,
        ) {
            // <CodeBlock renderList_1>
            ui.list {
                noui.item { +"Apples" }
                noui.item { +"Pears" }
                noui.item { +"Oranges" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderList_2,
        ) {
            // <CodeBlock renderList_2>
            ui.list {
                noui.item {
                    icon.fruitapple()
                    noui.content { +"Apples" }
                }
                noui.item {
                    icon.venus()
                    noui.content { +"Venus" }
                }
                noui.item {
                    icon.cogs()
                    noui.content {
                        a(href = "https://example/com") { +"Settings" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderList_3,
        ) {
            // <CodeBlock renderList_3>
            ui.divided.list {
                noui.item {
                    icon.large.middle.aligned.github()
                    noui.content {
                        noui.header {
                            a(href = "https://github.com/peekandpoke/kraft") { +"PeekAndPoke/kraft" }
                        }
                        noui.description {
                            +"Updated 10 mins ago"
                        }
                    }
                }
                noui.item {
                    icon.large.middle.aligned.github()
                    noui.content {
                        noui.header {
                            a(href = "https://github.com/peekandpoke/ultra") { +"PeekAndPoke/ultra" }
                        }
                        noui.description {
                            +"Updated 10 mins ago"
                        }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderList_4,
        ) {
            // <CodeBlock renderList_4>
            ui.list {
                noui.item {
                    icon.folder()
                    noui.content {
                        noui.header { +"src" }
                        noui.description { +"Source files" }
                        noui.list {
                            noui.item {
                                icon.folder()
                                noui.content {
                                    noui.header { +"site" }
                                    noui.description { +"Site's theme" }
                                }
                            }
                            noui.item {
                                icon.folder()
                                noui.content {
                                    noui.header { +"themes" }
                                    noui.description { +"Packages theme files" }
                                    noui.list {
                                        noui.item {
                                            icon.folder()
                                            noui.content {
                                                noui.header { +"default" }
                                                noui.description { +"Default theme" }
                                            }
                                        }
                                        noui.item {
                                            icon.folder()
                                            noui.content {
                                                noui.header { +"my_theme" }
                                                noui.description { +"My theme" }
                                            }
                                        }
                                    }
                                }
                            }
                            noui.item {
                                icon.file()
                                noui.content {
                                    noui.header { +"theme.config" }
                                    noui.description { +"Theme configuration" }
                                }
                            }
                        }
                    }
                }
                noui.item {
                    icon.folder()
                    noui.content {
                        noui.header { +"dist" }
                        noui.description { +"Compiles files" }
                        noui.list {
                            noui.item {
                                icon.folder()
                                noui.content {
                                    noui.header { +"components" }
                                    noui.description { +"Individuals component files" }
                                }
                            }
                        }
                    }
                }
                noui.item {
                    icon.file()
                    noui.content {
                        noui.header { +"semantic.json" }
                        noui.description { +"Contains build settings" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBulletedList() = example {
        ui.header H3 { +"Bulleted" }

        p { +"A list can mark items with a bullet" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderBulletedList_1,
        ) {
            // <CodeBlock renderBulletedList_1>
            ui.bulleted.list {
                noui.item { +"Gaining Access" }
                noui.item { +"Inviting Friends" }
                noui.item {
                    +"Benefits"
                    noui.list {
                        noui.item { a(href = "https://example.com") { +"List to somewhere" } }
                        noui.item { +"Rebates" }
                        noui.item { +"Discounts" }
                    }
                }
                noui.item { +"Warranty" }
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"For convenience, a simple bulleted list can also use "
            ui.label { +"ul" }
            +" tag."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderBulletedList_2,
        ) {
            // <CodeBlock renderBulletedList_2>
            ui.list Ul {
                li { +"Gaining Access" }
                li { +"Inviting Friends" }
                li {
                    +"Benefits"
                    ul {
                        li { a(href = "https://example.com") { +"List to somewhere" } }
                        li { +"Rebates" }
                        li { +"Discounts" }
                    }
                }
                li { +"Warranty" }
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"A bulleted list can be horizontal"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderBulletedList_3,
        ) {
            // <CodeBlock renderBulletedList_3>
            ui.horizontal.bulleted.list {
                noui.item { +"Apples" }
                noui.item { +"Pears" }
                noui.item { +"Oranges" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderOrderedList() = example {
        ui.header H3 { +"Ordered" }

        p { +"A list can be ordered numerically." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderOrderedList_1,
        ) {
            // <CodeBlock renderOrderedList_1>
            ui.ordered.list {
                noui.item { +"Gaining Access" }
                noui.item { +"Inviting Friends" }
                noui.item {
                    +"Benefits"
                    noui.list {
                        noui.item { a(href = "https://example.com") { +"List to somewhere" } }
                        noui.item { +"Rebates" }
                        noui.item { +"Discounts" }
                    }
                }
                noui.item { +"Warranty" }
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"For convenience, a simple bulleted list can also use "
            ui.label { +"ol" }
            +" tag."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderOrderedList_2,
        ) {
            // <CodeBlock renderOrderedList_2>
            ui.list Ol {
                li { +"Gaining Access" }
                li { +"Inviting Friends" }
                li {
                    +"Benefits"
                    ol {
                        li { a(href = "https://example.com") { +"List to somewhere" } }
                        li { +"Rebates" }
                        li { +"Discounts" }
                    }
                }
                li { +"Warranty" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderOrderedList_3,
        ) {
            // <CodeBlock renderOrderedList_3>
            ui.list Ol {
                li { value = "*"; +"Gaining Access" }
                li { value = "*"; +"Inviting Friends" }
                li {
                    value = "*"; +"Benefits"
                    ol {
                        li { value = "-"; a(href = "https://example.com") { +"List to somewhere" } }
                        li { value = "-"; +"Rebates" }
                        li { value = "-"; +"Discounts" }
                    }
                }
                li { value = "x"; +"Warranty" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLinkList() = example {
        ui.header H3 { +"Link" }

        p { +"A list can be specially formatted for navigation links." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderLinkList_1,
        ) {
            // <CodeBlock renderLinkList_1>
            ui.link.list {
                noui.active.item { +"Home" }
                noui.item { +"About" }
                noui.item { +"Jobs" }
                noui.item { +"Team" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderImageContent() = example {
        ui.header H3 { +"Image" }

        p { +"A list item can contain an image" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderImageContent,
        ) {
            // <CodeBlock renderImageContent>
            ui.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHeaderContent() = example {
        ui.header H3 { +"Header" }

        p { +"A list item can contain a header" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderHeaderContent,
        ) {
            // <CodeBlock renderHeaderContent>
            ui.list {
                noui.item {
                    noui.header { +"New York City" }
                    +"A lovely city"
                }
                noui.item {
                    noui.header { +"Chicago" }
                    +"Also quite a lovely city "
                }
                noui.item {
                    noui.header { +"Los Angeles" }
                    +"Sometimes can be a lovely city "
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDescriptionContent() = example {
        ui.header H3 { +"Description" }

        p { +"A list item can contain a description" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderDescriptionContent,
        ) {
            // <CodeBlock renderDescriptionContent>
            ui.list {
                noui.item {
                    icon.big.map_marker_alternate()
                    noui.content {
                        noui.header A {
                            href = "https://example.com"; +"Krolewskie Jadlo"
                        }
                        noui.description {
                            +"An excellent polish restaurant, quick delivery and hearty, filling meals."
                        }
                    }
                }
                noui.item {
                    icon.big.map_marker_alternate()
                    noui.content {
                        noui.header A {
                            href = "https://example.com"; +"Xian Famous Foods"
                        }
                        noui.description {
                            +"A taste of Shaanxi's delicious culinary traditions, with delights like spicy cold "
                            +"noodles and lamb burgers."
                        }
                    }
                }
                noui.item {
                    icon.big.map_marker_alternate()
                    noui.content {
                        noui.header A {
                            href = "https://example.com"; +"Sapporo Haru"
                        }
                        noui.description {
                            +"Greenpoint's best choice for quick and delicious sushi."
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontalList() = example {
        ui.header H3 { +"Horizontal" }

        p { +"A list can be formatted to have items appear horizontally" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderHorizontalList_1,
        ) {
            // <CodeBlock renderHorizontalList_1>
            ui.horizontal.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderHorizontalList_2,
        ) {
            // <CodeBlock renderHorizontalList_2>
            ui.ordered.horizontal.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderHorizontalList_3,
        ) {
            // <CodeBlock renderHorizontalList_3>
            ui.horizontal.bulleted.link.list {
                noui.item { +"Terms and Conditions" }
                noui.item { +"Privacy Policy" }
                noui.item { +"Contract Us" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInvertedList() = example {
        ui.header H3 { +"Inverted" }

        p { +"A list can be inverted to appear on a dark background" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderInvertedList,
        ) {
            // <CodeBlock renderInvertedList>
            ui.inverted.segment {
                ui.inverted.divided.list {
                    noui.item {
                        noui.header { +"Snickerdoodle" }
                        +"An excellent companion"
                    }
                    noui.item {
                        noui.header { +"Poodle" }
                        +"A poodle, its pretty basic"
                    }
                    noui.item {
                        noui.header { +"Paulo" }
                        +"He's also a dog"
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSelectionList() = example {
        ui.header H3 { +"Selection" }

        p { +"A selection list formats list items as possible choices" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderSelectionList,
        ) {
            // <CodeBlock renderSelectionList>
            ui.middle.aligned.selection.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAnimatedList() = example {
        ui.header H3 { +"Animated" }

        p { +"A list can animate to set the current item apart from the list" }

        ui.info.message {
            +"Be sure content can fit on one line when using the animated variation, "
            +"otherwise text content will reflow when hovered. "
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderAnimatedList,
        ) {
            // <CodeBlock renderAnimatedList>
            ui.middle.aligned.animated.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRelaxedList() = example {
        ui.header H3 { +"Relaxed" }

        p { +"A list can relax its padding to provide more negative space" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderRelaxedList_1,
        ) {
            // <CodeBlock renderRelaxedList_1>
            ui.relaxed.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderRelaxedList_2,
        ) {
            // <CodeBlock renderRelaxedList_2>
            ui.relaxed.horizontal.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderRelaxedList_3,
        ) {
            // <CodeBlock renderRelaxedList_3>
            ui.very.relaxed.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderRelaxedList_4,
        ) {
            // <CodeBlock renderRelaxedList_4>
            ui.horizontal.very.relaxed.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                        noui.description { +LoremIpsum(5) }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                        noui.description { +LoremIpsum(5) }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDividedList() = example {
        ui.header H3 { +"Divided" }

        p { +"A list can show divisions between content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderDividedList,
        ) {
            // <CodeBlock renderDividedList>
            ui.middle.aligned.divided.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderDividedList_2,
        ) {
            // <CodeBlock renderDividedList_2>
            ui.middle.aligned.horizontal.divided.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCelledList() = example {
        ui.header H3 { +"Celled" }

        p { +"A list can divide its items into cells" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderCelledList,
        ) {
            // <CodeBlock renderCelledList>
            ui.middle.aligned.divided.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderCelledList_2,
        ) {
            // <CodeBlock renderCelledList_2>
            ui.middle.aligned.horizontal.celled.list {
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content {
                        noui.header { +"Elliot" }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content {
                        noui.header {
                            a(href = "https://example.com") { +"Eve" }
                        }
                    }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content {
                        noui.header { +"Molly" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSizedList() = example {
        ui.header H3 { +"Size" }

        p { +"A list can vary in size" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderSizedList_1,
        ) {
            // <CodeBlock renderSizedList_1>
            allSizes.forEach { size ->
                ui.basic.segment {
                    ui.size().middle.aligned.horizontal.list {
                        noui.item {
                            ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                            noui.content {
                                noui.header { +"Elliot" }
                            }
                        }
                        noui.item {
                            ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                            noui.content {
                                noui.header {
                                    a(href = "https://example.com") { +"Eve" }
                                }
                            }
                        }
                        noui.item {
                            ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                            noui.content {
                                noui.header { +"Molly" }
                            }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVerticalAlignment() = example {
        ui.header H3 { +"Vertically Aligned" }

        p { +"An element inside a list can be vertically aligned" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderVerticalAlignment,
        ) {
            // <CodeBlock renderVerticalAlignment>
            ui.horizontal.list {
                noui.item {
                    ui.avatar.image Img { src = "images/wireframe/image.png" }
                    noui.top.aligned.content { +"Top aligned" }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/wireframe/image.png" }
                    noui.middle.aligned.content { +"Middle aligned" }
                }
                noui.item {
                    ui.avatar.image Img { src = "images/wireframe/image.png" }
                    noui.bottom.aligned.content { +"Bottom aligned" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloatedContent() = example {
        ui.header H3 { +"Floated" }

        p { +"A list, or an element inside a list can be floated left or right" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderFloatedContent,
        ) {
            // <CodeBlock renderFloatedContent>
            ui.divided.list {
                noui.item {
                    ui.right.floated.content {
                        ui.button { +"Add" }
                    }
                    ui.avatar.image Img { src = "images/avatar2/large/molly.png" }
                    noui.content { +"Molly" }
                }
                noui.item {
                    ui.right.floated.content {
                        ui.button { +"Add" }
                    }
                    ui.avatar.image Img { src = "images/avatar2/large/eve.png" }
                    noui.content { +"Eve" }
                }
                noui.item {
                    ui.right.floated.content {
                        ui.button { +"Add" }
                    }
                    ui.avatar.image Img { src = "images/avatar2/large/elliot.jpg" }
                    noui.content { +"Elliot" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_list_ListPage_kt_renderFloatedContent_2,
        ) {
            // <CodeBlock renderFloatedContent_2>
            ui.right.floated.horizontal.list {
                noui.disabled.item { +"© GitHub, Inc." }
                noui.item A { href = "https://example.com"; +"Terms" }
                noui.item A { href = "https://example.com"; +"Privacy" }
                noui.item A { href = "https://example.com"; +"Contact" }
            }
            ui.horizontal.list {
                noui.item A { href = "https://example.com"; +"About Us" }
                noui.item A { href = "https://example.com"; +"Jobs" }
            }
            // </CodeBlock>
        }
    }
}
