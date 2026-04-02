@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.collections.breadcrumb

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.BreadcrumbPage() = comp {
    BreadcrumbPage(it)
}

class BreadcrumbPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Breadcrumb")

        ui.basic.segment {
            ui.dividing.header H1 { +"Breadcrumb" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/breadcrumb.html")

            ui.dividing.header H2 { +"Types" }

            renderBreadcrumb()

            ui.dividing.header H2 { +"Content" }

            renderDivider()
            renderSection()
            renderLink()

            ui.dividing.header H2 { +"States" }

            renderActive()

            ui.dividing.header H2 { +"Variations" }

            renderInverted()
            renderSize()
        }
    }

    private fun FlowContent.renderBreadcrumb() = example {
        ui.header H3 { +"Breadcrumb" }

        p { +"A standard breadcrumb." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderBreadcrumb,
        ) {
            // <CodeBlock renderBreadcrumb>
            ui.breadcrumb {
                a(classes = "section") { +"Home" }
                noui.divider { +"/" }
                a(classes = "section") { +"Store" }
                noui.divider { +"/" }
                noui.active.section { +"T-Shirt" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDivider() = example {
        ui.header H3 { +"Divider" }

        p { +"A breadcrumb can contain a divider to show the relationship between sections." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderDivider_1,
        ) {
            // <CodeBlock renderDivider_1>
            ui.breadcrumb {
                a(classes = "section") { +"Home" }
                noui.divider { +"/" }
                a(classes = "section") { +"Registration" }
                noui.divider { +"/" }
                noui.active.section { +"Personal Information" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderDivider_2,
        ) {
            // <CodeBlock renderDivider_2>
            ui.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.angle_right.divider()
                a(classes = "section") { +"Registration" }
                icon.angle_right.divider()
                noui.active.section { +"Personal Information" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSection() = example {
        ui.header H3 { +"Section" }

        p { +"A breadcrumb can contain sections that can either be formatted as a link or text." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderSection,
        ) {
            // <CodeBlock renderSection>
            ui.breadcrumb {
                noui.section { +"Home" }
                noui.divider { +"/" }
                noui.active.section { +"Search" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLink() = example {
        ui.header H3 { +"Link" }

        p { +"A section may be linkable or contain a link." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderLink,
        ) {
            // <CodeBlock renderLink>
            ui.breadcrumb {
                a(classes = "section") { +"Home" }
                noui.divider { +"/" }
                noui.active.section {
                    +"Search for: "
                    a { +"paper towels" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderActive() = example {
        ui.header H3 { +"Active" }

        p { +"A section can be active." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderActive,
        ) {
            // <CodeBlock renderActive>
            ui.breadcrumb {
                a(classes = "section") { +"Products" }
                icon.chevron_right.divider()
                noui.active.section { +"Paper Towels" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H3 { +"Inverted" }

        p { +"A breadcrumb can be inverted." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.segment {
                ui.inverted.breadcrumb {
                    a(classes = "section") { +"Home" }
                    icon.chevron_right.divider()
                    a(classes = "section") { +"Registration" }
                    icon.chevron_right.divider()
                    noui.active.section { +"Personal Information" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"A breadcrumb can vary in size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_breadcrumb_BreadcrumbPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.mini.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            ui.divider {}

            ui.tiny.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            ui.divider {}

            ui.small.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            ui.divider {}

            ui.large.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            ui.divider {}

            ui.big.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            ui.divider {}

            ui.huge.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            ui.divider {}

            ui.massive.breadcrumb {
                a(classes = "section") { +"Home" }
                icon.chevron_right.divider()
                a(classes = "section") { +"Registration" }
                icon.chevron_right.divider()
                noui.active.section { +"Personal Information" }
            }
            // </CodeBlock>
        }
    }
}
