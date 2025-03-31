package de.peekandpoke.kraft.examples.fomanticui.pages.collections.table

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
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
fun Tag.TablePage() = comp {
    TablePage(it)
}

class TablePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Table")

        ui.basic.segment {
            ui.dividing.header H1 { +"Table" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/table.html")

            ui.dividing.header H2 { +"Types" }

            renderTable()
            renderDefinition()
            renderStructured()

            ui.dividing.header H2 { +"Types" }

            renderPositiveOrNegative()
            renderError()
            renderWarning()
            renderActive()
            renderDisabled()
            renderColoredCells()
            renderMarked()

            ui.dividing.header H2 { +"Variations" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/table.html#single-line")

        }
    }

    private fun FlowContent.renderTable() = example {
        ui.header H3 { +"Table" }

        p { +"A standard table" }

        ui.info.message {
            +" Tables will automatically stack their layouts for mobile devices. To disable this behavior, use the "
            ui.label { +"unstackable" }
            +" variation or "
            ui.label { +"tablet stackable" }
            +" to allow responsive adjustments for tablet. "
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderTable_1,
        ) {
            // <CodeBlock renderTable_1>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Age" }
                        th { +"Job" }
                    }
                }
                tbody {
                    tr {
                        td { +"James" }
                        td { +"24" }
                        td { +"Engineer" }
                    }
                    tr {
                        td { +"Jill" }
                        td { +"26" }
                        td { +"Engineer" }
                    }
                    tr {
                        td { +"Elyse" }
                        td { +"24" }
                        td { +"Engineer" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderTable_2,
        ) {
            // <CodeBlock renderTable_2>
            ui.celled.padded.table Table {
                thead {
                    tr {
                        th(classes = "single line") { +"Evidence Rating" }
                        th { +"Effect" }
                        th { +"Consensus" }
                        th { +"Comments" }
                    }
                }
                tbody {
                    tr {
                        td { ui.center.aligned.header H2 { +"A" } }
                        td(classes = "single line") { +"Power Output" }
                        td(classes = "right aligned") {
                            +"80%"
                            br()
                            a(href = "#") { +"18 studies" }
                        }
                        td { +LoremIpsum(15) }
                    }
                    tr {
                        td { ui.center.aligned.header H2 { +"A" } }
                        td(classes = "single line") { +"Weight" }
                        td(classes = "right aligned") {
                            +"100%"
                            br()
                            a(href = "#") { +"65 studies" }
                        }
                        td { +LoremIpsum(15) }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderTable_3,
        ) {
            // <CodeBlock renderTable_3>
            ui.very.basic.collapsing.celled.table Table {
                thead {
                    tr {
                        th { +"Employee" }
                        th { +"Correct Guesses" }
                    }
                }
                tbody {
                    tr {
                        td {
                            ui.image.header.header H4 {
                                ui.mini.rounded.image Img { src = "images/avatar2/large/eve.png" }
                                noui.content {
                                    +"Eve"
                                    noui.sub.header { +"Human Resources" }
                                }
                            }
                        }
                        td { +"22" }
                    }
                    tr {
                        td {
                            ui.image.header.header H4 {
                                ui.mini.rounded.image Img { src = "images/avatar2/large/kristy.png" }
                                noui.content {
                                    +"Kristy"
                                    noui.sub.header { +"Fabric Design" }
                                }
                            }
                        }
                        td { +"17" }
                    }
                    tr {
                        td {
                            ui.image.header.header H4 {
                                ui.mini.rounded.image Img { src = "images/avatar2/large/matthew.png" }
                                noui.content {
                                    +"Matthew"
                                    noui.sub.header { +"Entertainment" }
                                }
                            }
                        }
                        td { +"11" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderTable_4,
        ) {
            // <CodeBlock renderTable_4>
            ui.celled.striped.table Table {
                thead {
                    tr {
                        th {
                            colSpan = "3"
                            +"Employee"
                        }
                    }
                }
                tbody {
                    tr {
                        td(classes = "collapsing") {
                            icon.folder()
                            +"node_modules"
                        }
                        td { +"Initial commit" }
                        td(classes = "right aligned collapsing") { +"15 hours ago" }
                    }
                    tr {
                        td(classes = "collapsing") {
                            icon.folder()
                            +"test"
                        }
                        td { +"test commit" }
                        td(classes = "right aligned collapsing") { +"9 hours ago" }
                    }
                    tr {
                        td(classes = "collapsing") {
                            icon.file()
                            +"package.json"
                        }
                        td { +"Initial commit" }
                        td(classes = "right aligned collapsing") { +"15 hours ago" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDefinition() = example {
        ui.header H3 { +"Definition" }

        p { +"A table may be formatted to emphasize a first column that defines a rows content" }

        ui.info.message {
            +"Definition tables are designed to display on a single background color. "
            +"You can adjust this by changing "
            ui.label { +"@definitionPageBackground" }
            +", or specifying a background color on the first cell"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderDefinition_1,
        ) {
            // <CodeBlock renderDefinition_1>
            ui.definition.table Table {
                thead {
                    tr {
                        th { }
                        th { +"Arguments" }
                        th { +"Description" }
                    }
                }
                tbody {
                    tr {
                        td { +"reset rating" }
                        td { +"None" }
                        td { +"Resets rating to default value" }
                    }
                    tr {
                        td { +"set rating" }
                        td { +"rating (integer)" }
                        td { +"Sets the current star rating to specified value" }
                    }
                    tr {
                        td { +"Elyse" }
                        td { +"24" }
                        td { +"Engineer" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderDefinition_2,
        ) {
            // <CodeBlock renderDefinition_2>
            ui.compact.celled.definition.table Table {
                thead {
                    tr {
                        th { }
                        th { +"Name" }
                        th { +"Registration date" }
                        th { +"E-mail address" }
                        th { +"Premium plan" }
                    }
                }
                tbody {
                    tr {
                        td(classes = "collapsing") {
                            ui.fitted.slider.checkbox {
                                input { type = InputType.checkBox }
                                label()
                            }
                        }
                        td { +"John Lilki" }
                        td { +"September 11, 2014" }
                        td { +"jhlilk22@example.com" }
                        td { +"No" }
                    }
                    tr {
                        td(classes = "collapsing") {
                            ui.fitted.slider.checkbox {
                                input { type = InputType.checkBox }
                                label()
                            }
                        }
                        td { +"Jamie Harington" }
                        td { +"January 11, 2014" }
                        td { +"jamieharingonton@example.com" }
                        td { +"Yes" }
                    }
                    tr {
                        td(classes = "collapsing") {
                            ui.fitted.slider.checkbox {
                                input { type = InputType.checkBox }
                                label()
                            }
                        }
                        td { +"Jill Lewis" }
                        td { +"May 11, 2014" }
                        td { +"jilsewris22@example.com" }
                        td { +"Yes" }
                    }
                }
                tfoot {
                    tr {
                        th {}
                        th {
                            colSpan = "4"
                            ui.right.floated.small.primary.labeled.icon.button {
                                icon.user()
                                +"Add user"
                            }
                            ui.small.button { +"Approve" }
                            ui.small.button { +"Approve All" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"If you work with the "
            ui.label { +"rowspan" }
            +" table technique, fomantic provides a helper class "
            ui.label { +"rowspanned" }
            +" to be used for an invisible td column to make sure the styling stays consistent"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderDefinition_3,
        ) {
            // <CodeBlock renderDefinition_3>
            ui.definition.table Table {
                tbody {
                    tr {
                        td { rowSpan = "2"; +"Category rowspanned" }
                        td { +"Row one" }
                    }
                    tr {
                        td(classes = "rowspanned") { }
                        td { +"Row two" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStructured() = example {

        ui.dividing.header H3 { +"Structured" }

        readTheFomanticUiDocs("https://fomantic-ui.com/collections/table.html#structured")
    }

    private fun FlowContent.renderPositiveOrNegative() = example {
        ui.header H3 { +"Positive / Negative" }

        p { +"A cell or row may let a user know whether a value is good or bad" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderPositiveOrNegative,
        ) {
            // <CodeBlock renderPositiveOrNegative>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Status" }
                        th { +"Notes" }
                    }
                }
                tbody {
                    tr {
                        td { +"No name" }
                        td { +"Unknown" }
                        td(classes = "negative") { +"None" }
                    }
                    tr(classes = "positive") {
                        td { +"Jimmy" }
                        td { icon.check(); +"Approved" }
                        td { +"None" }
                    }
                    tr(classes = "negative") {
                        td { +"Jamie" }
                        td { icon.times(); +"Rejected" }
                        td { +"None" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderError() = example {
        ui.header H3 { +"Error" }

        p { +"A cell or row may call attention to an error or a negative value" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderError,
        ) {
            // <CodeBlock renderError>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Status" }
                        th { +"Notes" }
                    }
                }
                tbody {
                    tr {
                        td { +"No name" }
                        td { +"Unknown" }
                        td(classes = "error") { icon.exclamation_circle(); +"Error" }
                    }
                    tr(classes = "error") {
                        td { +"Jimmy" }
                        td { icon.check(); +"Approved" }
                        td { +"None" }
                    }
                    tr {
                        td { +"Jamie" }
                        td { icon.times(); +"Rejected" }
                        td { +"None" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderWarning() = example {
        ui.header H3 { +"Warning" }

        p { +"A cell or row may warn a user" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderWarning,
        ) {
            // <CodeBlock renderWarning>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Status" }
                        th { +"Notes" }
                    }
                }
                tbody {
                    tr {
                        td { +"No name" }
                        td { +"Unknown" }
                        td(classes = "warning") { icon.exclamation_circle(); +"Error" }
                    }
                    tr(classes = "warning") {
                        td { +"Jimmy" }
                        td { icon.check(); +"Approved" }
                        td { +"None" }
                    }
                    tr {
                        td { +"Jamie" }
                        td { icon.times(); +"Rejected" }
                        td { +"None" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderActive() = example {
        ui.header H3 { +"Active" }

        p { +"A cell or row can be active or selected by a user" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderActive,
        ) {
            // <CodeBlock renderActive>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Status" }
                        th { +"Notes" }
                    }
                }
                tbody {
                    tr {
                        td { +"No name" }
                        td { +"Unknown" }
                        td(classes = "active") { icon.exclamation_circle(); +"Error" }
                    }
                    tr(classes = "active") {
                        td { +"Jimmy" }
                        td { icon.check(); +"Approved" }
                        td { +"None" }
                    }
                    tr {
                        td { +"Jamie" }
                        td { icon.times(); +"Rejected" }
                        td { +"None" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabled() = example {
        ui.header H3 { +"Disabled" }

        p { +"A cell can be disabled" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderDisabled,
        ) {
            // <CodeBlock renderDisabled>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Status" }
                        th { +"Notes" }
                    }
                }
                tbody {
                    tr {
                        td { +"No name" }
                        td { +"Unknown" }
                        td(classes = "disabled") { icon.exclamation_circle(); +"Error" }
                    }
                    tr(classes = "disabled") {
                        td { +"Jimmy" }
                        td { icon.check(); +"Approved" }
                        td { +"None" }
                    }
                    tr {
                        td { +"Jamie" }
                        td { icon.times(); +"Rejected" }
                        td { +"None" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColoredCells() = example {
        ui.header H3 { +"Colored Cells" }

        p { +"A cell can be styled by the central color palette colors" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_table_TablePage_kt_renderColoredCells,
        ) {
            // <CodeBlock renderColoredCells>
            ui.celled.table Table {
                thead {
                    tr {
                        th { +"Name" }
                        th { +"Status" }
                        th { +"Notes" }
                    }
                }
                tbody {
                    tr {
                        td(classes = "orange") { +"No name" }
                        td { +"Unknown" }
                        td(classes = "disabled") { icon.exclamation_circle(); +"Error" }
                    }
                    tr(classes = "blue") {
                        td { +"Jimmy" }
                        td { icon.check(); +"Approved" }
                        td { +"None" }
                    }
                    tr {
                        td { +"Jamie" }
                        td { icon.times(); +"Rejected" }
                        td(classes = "green") { +"None" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderMarked() = example {

        ui.dividing.header H3 { +"Marked" }

        readTheFomanticUiDocs("https://fomantic-ui.com/collections/table.html#marked")
    }
}
