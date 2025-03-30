@file:Suppress("DuplicatedCode")

package de.peekandpoke.kraft.examples.fomanticui.pages.collections.grid

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.examples.fomanticui.routes
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.GridPage() = comp {
    GridPage(it)
}

class GridPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Grid")

        ui.basic.segment {
            ui.dividing.header H1 { +"Grid" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/grid.html")

            ui.dividing.header H2 { +"Grids" }

            renderGrids()
            renderColumns()
            renderRows()
            renderGutters()
            renderNegativeMargins()
            renderPageGrids()

            ui.dividing.header H2 { +"Columns" }

            renderAutomaticFlow()
            renderColumnContent()
            renderColumnWidths()

            ui.dividing.header H2 { +"Rows" }

            renderRowGrouping()
            renderRowClearingContent()
            renderRowSpecialGrids()

            ui.dividing.header H2 { +"Varying Grids" }

            renderNestingGrids()
            renderColored()
            renderAutomaticColumnCount()
            renderCenteringContent()

            ui.dividing.header H2 { +"Responsive Grids" }

            renderResponsiveContainers()
            renderResponsiveStackable()
            renderResponsiveReverseOrder()
            renderResponsiveDoubling()
            renderResponsiveManualTweaks()
        }
    }

    private fun FlowContent.renderGrids() = example {
        ui.header H3 { +"Grids" }

        p { +"Using a grid makes content appear to flow more naturally on your page." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderGrids,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderGrids>
                ui.grid {
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColumns() = example {
        ui.header H3 { +"Columns" }

        p {
            +"""
                Grids divide horizontal space into indivisible units called "columns". All columns in a grid must 
                specify their width as proportion of the total available row width.
            """.trimIndent()
        }

        p {
            +"All grid systems choose an arbitrary column count to allow per row. Fomantic's default theme uses "
            b { +"16 columns" }
        }

        p {
            +"The example below shows four "
            ui.label { +"four wide" }
            +" columns will fit in the first row, "
            ui.label { +"16 / 4 = 4" }
            +", and three various sized columns in the second row. "
            ui.label { +"2 + 8 + 6 = 16" }
        }

        p {
            +"The default column count, and other arbitrary features of grids can be changed by adjusting Fomantic "
            +"UI's underlying "
            a(href = "https://fomantic-ui.com/usage/theming.html") { +"theming variables." }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderColumns,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderColumns>
                ui.grid {
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.two.wide.column {}
                    noui.eight.wide.column {}
                    noui.six.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRows() = example {
        ui.header H3 { +"Rows" }

        p { +"Rows are groups of columns which are aligned horizontally." }

        p {
            +"Rows can either be explicit, marked with an additional "
            ui.label { +"row" }
            +" element, or implicit, automatically occurring when no more space is left in a previous row."
        }

        p {
            +"After each group of columns vertical spacing is added to separate each group of columns, "
            +"creating vertical rhythm."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderRows,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderRows>
                ui.grid {
                    noui.row {
                        noui.four.wide.column {}
                        noui.four.wide.column {}
                        noui.four.wide.column {}
                    }
                    noui.row {
                        noui.four.wide.column {}
                        noui.four.wide.column {}
                        noui.four.wide.column {}
                        noui.four.wide.column {}
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderGutters() = example {
        ui.header H3 { +"Gutters" }

        p {
            +"Grid columns are separated by areas of white space referred to as \"gutters\". Gutters improve "
            +"legibility by providing, "
            a(href = "https://en.wikipedia.org/wiki/Negative_space") { +"negative space" }
            +" between page elements."
        }

        p {
            +"Gutters remain a constant size regardless of the width of the grid, or how many columns are in a row. "
            +"To increase the size of gutters in a particular grid, you can use a "
            ui.label { +"relaxed grid" }
            +" variation."
        }

        p {
            +"After each group of columns vertical spacing is added to separate each group of columns, "
            +"creating vertical rhythm."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderGutters,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderGutters>
                ui.grid {
                    noui.two.column.row {
                        noui.column {}
                        noui.column {}
                    }
                    noui.three.column.row {
                        noui.column {}
                        noui.column {}
                        noui.column {}
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderGutters_2,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderGutters_2>
                ui.relaxed.grid {
                    noui.two.column.row {
                        noui.column {}
                        noui.column {}
                    }
                    noui.three.column.row {
                        noui.column {}
                        noui.column {}
                        noui.column {}
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderGutters_3,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderGutters_3>
                ui.very.relaxed.grid {
                    noui.two.column.row {
                        noui.column {}
                        noui.column {}
                    }
                    noui.three.column.row {
                        noui.column {}
                        noui.column {}
                        noui.column {}
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderNegativeMargins() = example {
        ui.header H3 { +"Gutters" }

        p {
            +"Since all grid columns include gutters, grids use "
            a(href = "http://csswizardry.com/2011/08/building-better-grid-systems/") { +"negative margins" }
            +" to make sure that the first and last columns sit flush with content outside the grid."
        }

        p {
            +"In the following example, you can see even though the top row has padding, the "
            ui.label { +"attached button" }
            +" still sits flush with the edge of the grid."
        }

        p {
            +"In some cases, like when a column or row is "
            ui.label { +"colored" }
            +", you may want to avoid using negative margins. You can do this by using a "
            ui.label { +"padded grid" }
            +" variation."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderNegativeMargins,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderNegativeMargins>
                ui.top.attached.button { +"Button before grid" }
                ui.grid {
                    noui.sixteen.wide.column {}
                    noui.ten.wide.column {}
                    noui.six.wide.column {}
                }
                ui.bottom.attached.button { +"Button after grid" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPageGrids() = example {
        ui.header H3 { +"Page Grids" }

        p { +"Grids are fluid and will automatically flow in size to take the maximum available width." }

        p {
            a(href = routes.elementsContainer()) { +"Containers" }
            +" are elements designed to limit page content to a reasonable maximum width for display based "
            +"on the size of the user's screen."
        }

        p {
            +"Using a "
            ui.label { +"ui grid container" }
            +" is the best way to include top-level page content inside a grid."
        }
    }

    private fun FlowContent.renderAutomaticFlow() = example {
        ui.header H3 { +"Automatic Flow" }

        p {
            +"Most grids do not need to specify rows. Content will automatically flow to the next row when all the "
            +"grid columns are taken in the current row."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderAutomaticFlow,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderAutomaticFlow>
                ui.grid {
                    noui.eight.wide.column {}
                    noui.eight.wide.column {}
                    noui.eight.wide.column {}
                    noui.eight.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColumnContent() = example {
        ui.header H3 { +"Column Content" }

        p {
            +"Since columns use padding to create gutters, content stylings should not be applied directly to "
            +"columns, but to elements inside of columns."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderColumnContent,
        ) {
            ui.basic.segment {
                // <CodeBlock renderColumnContent>
                ui.three.column.grid {
                    noui.column {
                        ui.segment {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                    }
                    noui.column {
                        ui.segment {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                    }
                    noui.column {
                        ui.segment {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColumnWidths() = example {
        ui.header H3 { +"Column Widths" }

        p {
            +"Column widths can be specified using "
            ui.label { +"(x) wide" }
            +" class names. If a column cannot fit in a row it will automatically flow to the next row"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderColumnWidths,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderColumnWidths>
                ui.grid {
                    noui.eight.wide.column {}
                    noui.eight.wide.column {}
                    noui.ten.wide.column {}
                    noui.six.wide.column {}
                    noui.four.wide.column {}
                    noui.twelve.wide.column {}
                    noui.two.wide.column {}
                    noui.fourteen.wide.column {}
                    noui.sixteen.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRowGrouping() = example {
        ui.header H3 { +"Grouping" }

        p { +"Row wrappers allow you to apply variations to a group of columns." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderRowGrouping,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderRowGrouping>
                ui.four.column.grid {
                    noui.two.column.row {
                        noui.column {}
                    }
                    noui.column {}
                    noui.column {}
                    noui.column {}
                    noui.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRowClearingContent() = example {
        ui.header H3 { +"Clearing Content" }

        p {
            +"Row wrappers will automatically clear previous columns, making them useful when using "
            ui.label { +"floated" }
            +" variations."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderRowClearingContent,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderRowClearingContent>
                ui.grid {
                    noui.four.column.row {
                        noui.left.floated.column {}
                        noui.right.floated.column {}
                    }
                    noui.three.wide.column {}
                    noui.eight.wide.column {}
                    noui.five.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRowSpecialGrids() = example {
        ui.header H3 { +"Special Grids" }

        p {
            +"Additionally, some types of grids, like "
            ui.label { +"divided" }
            +" or "
            ui.label { +"celled" }
            +" require row wrappers to apply formatting correctly."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderRowSpecialGrids,
        ) {
            ui.basic.segment {
                // <CodeBlock renderRowSpecialGrids>
                ui.internally.celled.grid {
                    noui.row {
                        noui.three.wide.column {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                        noui.ten.wide.column {
                            ui.fitted.image Img { src = "images/wireframe/short-paragraph.png" }
                        }
                        noui.three.wide.column {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                    }
                    noui.row {
                        noui.three.wide.column {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                        noui.ten.wide.column {
                            ui.fitted.image Img { src = "images/wireframe/media-paragraph.png" }
                            ui.fitted.image Img { src = "images/wireframe/short-paragraph.png" }
                        }
                        noui.three.wide.column {
                            ui.fitted.image Img { src = "images/wireframe/image.png" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderNestingGrids() = example {
        ui.header H3 { +"Nesting Grids" }

        p { +"Grids can be placed inside of other grids, letting you sub-divide columns." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderNestingGrids,
        ) {
            ui.basic.segment.with("nested-grid-docs") {
                // <CodeBlock renderNestingGrids>
                ui.two.column.grid {
                    noui.column {
                        ui.three.column.grid {
                            noui.column {}
                            noui.column {}
                            noui.column {}
                        }
                    }
                    noui.column {}
                }
                ui.two.column.grid {
                    noui.column {}
                    noui.column {
                        ui.grid {
                            noui.six.wide.column {}
                            noui.ten.wide.column {}
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColored() = example {
        ui.header H3 { +"Colored" }

        p {
            +"Grids can use named colors variations to add background colors, but only with "
            ui.label { +"padded grid" }
            +" that do not include negative margins."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderColored,
        ) {
            ui.basic.segment {
                // <CodeBlock renderColored>
                ui.equal.width.center.aligned.padded.grid {
                    noui.row {
                        noui.olive.column { +"Olive" }
                        noui.black.column { +"Black" }
                    }
                    noui.row {
                        css { backgroundColor = Color("#869D05") }
                        noui.column { +"Custom colored row" }
                    }
                    noui.row {
                        noui.orange.column { +"Olive" }
                        noui.red.column { +"Black" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAutomaticColumnCount() = example {
        ui.header H3 { +"Automatic Column Count" }

        p {
            +"The "
            ui.label { +"equal width" }
            +" variation will automatically divide column width evenly. This is useful with dynamic content "
            +"where you do not know the column count in advance."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderAutomaticColumnCount,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderAutomaticColumnCount>
                ui.equal.width.grid {
                    noui.column {}
                    noui.column {}
                    noui.column {}
                    noui.equal.width.row {
                        noui.column {}
                        noui.column {}
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCenteringContent() = example {
        ui.header H3 { +"Centering Content" }

        p {
            +"If a row does not take up all sixteen grid columns, you can use a "
            ui.label { +"centered" }
            +" variation to center the column contents inside the grid."
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderAutomaticColumnCount,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderCenteringContent>
                ui.two.column.centered.grid {
                    noui.column {}
                    noui.four.column.centered.row {
                        noui.column {}
                        noui.column {}
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderResponsiveContainers() = example {
        ui.header H3 { +"Centering Content" }

        p {
            +"A "
            a(href = routes.elementsContainer()) { +"container" }
            +" can be used alongside a grid to provide a responsive, fixed width container for wrapping the "
            +"contents of a page."
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderResponsiveContainers,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderResponsiveContainers>
                ui.grid.container {
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderResponsiveStackable() = example {
        ui.header H3 { +"Stackable" }

        p {
            +"A "
            ui.label { +"stackable grid" }
            +" will automatically stack rows to a single columns on mobile devices"
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderResponsiveStackable,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderResponsiveStackable>
                ui.stackable.grid {
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                    noui.four.wide.column {}
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderResponsiveReverseOrder() = example {
        ui.header H3 { +"Reverse Order" }

        p {
            +"Fomantic includes special "
            ui.label { +"reversed" }
            +" variations that allow you to reverse the order of columns or rows by device"
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderResponsiveReverseOrder,
        ) {
            ui.basic.segment {
                // <CodeBlock renderResponsiveReverseOrder>
                ui.mobile.reversed.equal.width.grid {
                    noui.column {
                        ui.segment { +"First" }
                    }
                    noui.column {
                        ui.segment { +"Second" }
                    }
                    noui.column {
                        ui.segment { +"Third" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderResponsiveDoubling() = example {
        ui.header H3 { +"Doubling" }

        p {
            +"A "
            ui.label { +"doubling" }
            +" grid will double column widths for each device jump."
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderResponsiveDoubling,
        ) {
            ui.basic.segment.with("grid-docs") {
                // <CodeBlock renderResponsiveDoubling>
                ui.grid {
                    noui.doubling.eight.column.row {
                        repeat(8) { noui.column() }
                    }
                    noui.doubling.six.column.row {
                        repeat(6) { noui.column() }
                    }
                    noui.doubling.four.column.row {
                        repeat(4) { noui.column() }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderResponsiveManualTweaks() = example {
        ui.header H3 { +"Manual Tweaks" }

        p {
            +"Although design patterns like "
            ui.label { +"doubling" }
            +" or "
            ui.label { +"stackable" }
            +" are useful at simplifying responsive styling, you can also manually tweak device presentation "
            +"by specifying "
            ui.label { +"(x) wide device" }
            +" or "
            ui.label { +"device only" }
            +" columns or rows."
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_collections_grid_GridPage_kt_renderResponsiveManualTweaks,
        ) {
            ui.basic.segment {
                // <CodeBlock renderResponsiveManualTweaks>
                ui.centered.grid {
                    noui.computer.only.row {
                        ui.column {
                            ui.segment { +"computer only" }
                        }
                    }
                    repeat(5) {
                        noui.six.wide.tablet.eight.wide.computer.column {
                            ui.segment { +"six wide tablet eight wide computer" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }
}
