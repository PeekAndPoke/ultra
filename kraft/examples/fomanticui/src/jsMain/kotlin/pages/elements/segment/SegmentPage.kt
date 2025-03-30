@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.segment

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.examples.fomanticui.helpers.shortParagraphWireFrame
import de.peekandpoke.kraft.examples.fomanticui.routes
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.fixture.LoremIpsum
import generated.ExtractedCodeBlocks
import kotlinx.html.*

@Suppress("FunctionName")
fun Tag.SegmentPage() = comp {
    SegmentPage(it)
}

class SegmentPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Segment")

        ui.basic.segment {
            ui.dividing.header H1 { +"Segment" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/segment.html")

            ui.dividing.header H2 { +"Types" }

            renderSegment()
            renderPlaceholderSegment()
            renderRaisedSegment()
            renderStackedSegment()
            renderVerticalSegment()

            ui.dividing.header H2 { +"Groups" }

            renderSegmentsGroup()
            renderNestedSegments()
            renderHorizontalSegments()
            renderHorizontalEqualWidthSegments()
            renderHorizontalStackableSegments()
            renderRaisedSegments()
            renderStackedSegments()
            renderPiledSegments()

            ui.dividing.header H2 { +"States" }

            renderDisabledSegment()
            renderLoadingSegment()

            ui.dividing.header H2 { +"Variations" }

            renderInvertedSegment()
            renderAttachedSegment()
            renderPaddedSegment()
            renderFittedSegment()
            renderCompactSegment()
            renderColoredSegment()
            renderEmphasisSegment()
            renderCircularSegment()
            renderClearingSegment()
            renderFloatedSegment()
            renderTextAlignmentSegment()
            renderBasicSegment()
        }
    }

    private fun FlowContent.renderSegment() = example {
        ui.header H3 { +"Segment" }

        p { +"A segment of content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderSegment,
        ) {
            // <CodeBlock renderSegment>
            ui.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPlaceholderSegment() = example {
        ui.header H3 { +"Placeholder Segment" }

        p { +"A segment can be used to reserve space for conditionally displayed content." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderPlaceholderSegment_1,
        ) {
            // <CodeBlock renderPlaceholderSegment_1>
            ui.placeholder.segment {
                ui.icon.header {
                    icon.file_pdf_outline()
                    +"No documents are listed for this customer."
                }
                ui.primary.button {
                    +"Add document"
                }
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"To use inline-block content inside a placeholder, wrap the content in "
            ui.label { +"inline" }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderPlaceholderSegment_2,
        ) {
            // <CodeBlock renderPlaceholderSegment_2>
            ui.placeholder.segment {
                ui.icon.header {
                    icon.search()
                    +"We don't have any documents matching your query ."
                }
                noui.inline {
                    ui.primary.button { +"Clear query" }
                    ui.button { +"Add document" }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderPlaceholderSegment_3,
        ) {
            // <CodeBlock renderPlaceholderSegment_3>
            ui.placeholder.segment {
                ui.two.column.stackable.center.aligned.grid {
                    ui.vertical.divider { +"Or" }
                    ui.column {
                        ui.icon.header {
                            icon.search()
                            +"Find country"
                        }
                        noui.field {
                            ui.search {
                                ui.icon.input {
                                    input { placeholder = "Search countries..." }
                                    icon.search()
                                }
                            }
                        }
                    }
                    ui.column {
                        ui.icon.header {
                            icon.world()
                            +"Add New Country"
                        }
                        ui.primary.button { +"Create" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRaisedSegment() = example {
        ui.header H3 { +"Raised" }

        p { +"A segment may be formatted to raise above the page." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderRaisedSegment,
        ) {
            // <CodeBlock renderRaisedSegment>
            ui.raised.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStackedSegment() = example {
        ui.header H3 { +"Stacked" }

        p { +"A segment can be formatted to show it contains multiple pages." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderStackedSegment_1,
        ) {
            // <CodeBlock renderStackedSegment_1>
            ui.stacked.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderStackedSegment_2,
        ) {
            // <CodeBlock renderStackedSegment_2>
            ui.tall.stacked.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVerticalSegment() = example {
        ui.header H3 { +"Vertical Segment" }

        p { +"A vertical segment formats content to be aligned as part of a vertical group." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderVerticalSegment,
        ) {
            // <CodeBlock renderVerticalSegment>
            ui.segment {
                ui.vertical.segment {
                    shortParagraphWireFrame()
                }
                ui.segment {
                    shortParagraphWireFrame()
                }
                ui.vertical.segment {
                    shortParagraphWireFrame()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSegmentsGroup() = example {
        ui.header H3 { +"Segments" }

        p { +"A group of segments can be formatted to appear together." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderSegmentsGroup_1,
        ) {
            // <CodeBlock renderSegmentsGroup_1>
            ui.segments {
                ui.segment {
                    +"Top"
                }
                ui.segment {
                    +"Middle"
                }
                ui.segment {
                    +"Middle"
                }
                ui.segment {
                    +"Middle"
                }
                ui.segment {
                    +"Bottom"
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderSegmentsGroup_2,
        ) {
            // <CodeBlock renderSegmentsGroup_2>
            ui.basic.segments {
                ui.segment {
                    +"Top"
                }
                ui.segment {
                    +"Middle"
                }
                ui.segment {
                    +"Middle"
                }
                ui.segment {
                    +"Middle"
                }
                ui.segment {
                    +"Bottom"
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderSegmentsGroup_3,
        ) {
            // <CodeBlock renderSegmentsGroup_3>
            ui.segments {
                ui.segment {
                    +"Top"
                }
                ui.red.segment {
                    +"Middle"
                }
                ui.blue.segment {
                    +"Middle"
                }
                ui.green.segment {
                    +"Middle"
                }
                ui.yellow.segment {
                    +"Bottom"
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderSegmentsGroup_4,
        ) {
            // <CodeBlock renderSegmentsGroup_4>
            ui.segments {
                ui.segment {
                    +"Top"
                }
                ui.secondary.segment {
                    +"Secondary Segment"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderNestedSegments() = example {
        ui.header H3 { +"Nested Segments" }

        p { +"A group of segments can be nested in another group of segments." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderNestedSegments,
        ) {
            // <CodeBlock renderNestedSegments>
            ui.segments {
                ui.segment {
                    +"Top"
                }
                ui.segments {
                    ui.segment {
                        +"Nested Top"
                    }
                    ui.segment {
                        +"Nested Middle"
                    }
                    ui.segment {
                        +"Nested Bottom"
                    }
                }
                ui.segment {
                    +"Top"
                }
                ui.horizontal.segments {
                    ui.segment {
                        +"Left"
                    }
                    ui.segment {
                        +"Center"
                    }
                    ui.segment {
                        +"Right"
                    }
                }
                ui.segment {
                    +"Bottom"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontalSegments() = example {
        ui.header H3 { +"Horizontal Segments" }

        p { +"A segment group can appear horizontally" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderHorizontalSegments_1,
        ) {
            // <CodeBlock renderHorizontalSegments_1>
            ui.horizontal.segments {
                ui.segment {
                    shortParagraphWireFrame()
                }
                ui.segment {
                    shortParagraphWireFrame()
                }
                ui.segment {
                    shortParagraphWireFrame()
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderHorizontalSegments_2,
        ) {
            // <CodeBlock renderHorizontalSegments_2>
            ui.basic.horizontal.segments {
                ui.segment {
                    shortParagraphWireFrame()
                }
                ui.segment {
                    shortParagraphWireFrame()
                }
                ui.segment {
                    shortParagraphWireFrame()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontalEqualWidthSegments() = example {
        ui.header H3 { +"Horizontal equal width Segments" }

        p { +"A horizontal segment group can automatically divide segments to be equal width" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderHorizontalEqualWidthSegments,
        ) {
            // <CodeBlock renderHorizontalEqualWidthSegments>
            ui.equal.width.horizontal.segments {
                ui.segment {
                    +LoremIpsum(2)
                }
                ui.segment {
                    +LoremIpsum(5)
                }
                ui.segment {
                    +LoremIpsum(8)
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderHorizontalStackableSegments() = example {
        ui.header H3 { +"Horizontal stackable Segments" }

        p { +"A horizontal segment group can automatically stack on smaller screens" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderHorizontalStackableSegments,
        ) {
            // <CodeBlock renderHorizontalStackableSegments>
            ui.horizontal.stackable.segments {
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRaisedSegments() = example {
        ui.header H3 { +"Raised Segments" }

        p { +"A group of segments can be raised" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderRaisedSegments,
        ) {
            // <CodeBlock renderRaisedSegments>
            ui.raised.segments {
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStackedSegments() = example {
        ui.header H3 { +"Stacked Segments" }

        p { +"A group of segments can be stacked" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderStackedSegments,
        ) {
            // <CodeBlock renderStackedSegments>
            ui.stacked.segments {
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPiledSegments() = example {
        ui.header H3 { +"Stacked Segments" }

        p { +"A group of segments can be piled" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderPiledSegments,
        ) {
            // <CodeBlock renderPiledSegments>
            ui.piled.segments {
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
                ui.segment {
                    +LoremIpsum(4)
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabledSegment() = example {
        ui.header H3 { +"Disabled Segment" }

        p { +"A segment may show its content is disabled" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderDisabledSegment,
        ) {
            // <CodeBlock renderDisabledSegment>
            ui.disabled.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLoadingSegment() = example {
        ui.header H3 { +"Loading Segment" }

        p { +"A segment may show its content is being loaded" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderLoadingSegment_1,
        ) {
            // <CodeBlock renderLoadingSegment_1>
            ui.loading.segment {
                shortParagraphWireFrame()
                shortParagraphWireFrame()
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }

        ui.info.message {
            +"The loader inherits the color of the segment, if you want to prevent this, add the "
            ui.label { +"usual" }
            +" class, so the loader color stays default, while the segment still gets its color"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderLoadingSegment_2,
        ) {
            // <CodeBlock renderLoadingSegment_2>
            ui.red.loading.segment {
                shortParagraphWireFrame()
            }
            ui.red.usual.loading.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }


    private fun FlowContent.renderInvertedSegment() = example {
        ui.header H3 { +"Inverted" }

        p { +"A segment can have its colors inverted for contrast" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderInvertedSegment_1,
        ) {
            // <CodeBlock renderInvertedSegment_1>
            ui.inverted.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAttachedSegment() = example {
        ui.header H3 { +"Attached" }

        p { +"A segment can be attached to other content on a page" }

        ui.info.message {
            +"Attached segments are designed to be used with other attached variations like "
            b { a(href = routes.elementsHeader()) { +"attached headers" } }
            +" or "
            b { a(href = routes.collectionsMessage()) { +"attached messages" } }
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderAttachedSegment_1,
        ) {
            // <CodeBlock renderAttachedSegment_1>
            ui.top.attached.segment {
                shortParagraphWireFrame()
            }
            ui.attached.segment {
                shortParagraphWireFrame()
            }
            ui.bottom.attached.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderAttachedSegment_2,
        ) {
            // <CodeBlock renderAttachedSegment_2>
            ui.top.attached.header {
                +"Header"
            }
            ui.attached.segment {
                shortParagraphWireFrame()
            }
            ui.bottom.attached.yellow.message {
                +LoremIpsum(10)
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderPaddedSegment() = example {
        ui.header H3 { +"Padded" }

        p { +"A segment can increase its padding" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderPaddedSegment_1,
        ) {
            // <CodeBlock renderPaddedSegment_1>
            ui.padded.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderPaddedSegment_2,
        ) {
            // <CodeBlock renderPaddedSegment_2>
            ui.very.padded.segment {
                shortParagraphWireFrame()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFittedSegment() = example {
        ui.header H3 { +"Fitted" }

        p { +"A segment can remove its padding, vertically or horizontally" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderFittedSegment,
        ) {
            // <CodeBlock renderFittedSegment>
            ui.fitted.segment {
                +"Fitted segment"
            }
            ui.horizontally.fitted.segment {
                +"Horizontally segment"
            }
            ui.vertically.fitted.segment {
                +"Vertically segment"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCompactSegment() = example {
        ui.header H3 { +"Compact" }

        p { +"A segment may take up only as much space as is necessary" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderCompactSegment_1,
        ) {
            // <CodeBlock renderCompactSegment_1>
            ui.compact.segment {
                +"Compact segment"
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderCompactSegment_2,
        ) {
            // <CodeBlock renderCompactSegment_2>
            ui.compact.segments {
                ui.segment {
                    +"Child segment"
                }
                ui.segment {
                    +"Child segment 2"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderColoredSegment() = example {
        ui.header H3 { +"Colored" }

        p { +"A segment can be colored" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderColoredSegment,
        ) {
            // <CodeBlock renderColoredSegment>
            ui.red.segment { +"red" }
            ui.orange.segment { +"orange" }
            ui.yellow.segment { +"yellow" }
            ui.olive.segment { +"olive" }
            ui.green.segment { +"green" }
            ui.teal.segment { +"teal" }
            ui.blue.segment { +"blue" }
            ui.violet.segment { +"violet" }
            ui.purple.segment { +"purple" }
            ui.pink.segment { +"pink" }
            ui.brown.segment { +"brown" }
            ui.grey.segment { +"grey" }
            ui.black.segment { +"black" }
            ui.white.segment { +"white" }
            // </CodeBlock>
        }

        ui.divider()

        p { +"These colors can be inverted" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderColoredSegment_2,
        ) {
            // <CodeBlock renderColoredSegment_2>
            ui.inverted.red.segment { +"red" }
            ui.inverted.orange.segment { +"orange" }
            ui.inverted.yellow.segment { +"yellow" }
            ui.inverted.olive.segment { +"olive" }
            ui.inverted.green.segment { +"green" }
            ui.inverted.teal.segment { +"teal" }
            ui.inverted.blue.segment { +"blue" }
            ui.inverted.violet.segment { +"violet" }
            ui.inverted.purple.segment { +"purple" }
            ui.inverted.pink.segment { +"pink" }
            ui.inverted.brown.segment { +"brown" }
            ui.inverted.grey.segment { +"grey" }
            ui.inverted.black.segment { +"black" }
            ui.inverted.white.segment { +"white" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderEmphasisSegment() = example {
        ui.header H3 { +"Emphasis" }

        p { +"A segment can be formatted to appear more or less noticeable" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderEmphasisSegment,
        ) {
            // <CodeBlock renderEmphasisSegment>
            ui.segment { +"Normal segment" }
            ui.secondary.segment { +"Secondary segment" }
            ui.tertiary.segment { +"Tertiary segment" }
            // </CodeBlock>
        }

        ui.divider()

        p {
            +"Inverted colors may also be more or less noticeable"
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderEmphasisSegment_2,
        ) {
            // <CodeBlock renderEmphasisSegment_2>
            ui.red.inverted.segment { +"Normal segment" }
            ui.red.inverted.secondary.segment { +"Secondary segment" }
            ui.red.inverted.tertiary.segment { +"Tertiary segment" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCircularSegment() = example {
        ui.header H3 { +"Circular" }

        p { +"A segment can be circular" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderCircularSegment,
        ) {
            // <CodeBlock renderCircularSegment>
            ui.circular.segment {
                ui.header H2 {
                    +"Buy now"
                    noui.sub.header { +"\$10.99" }
                }
            }
            ui.inverted.red.circular.segment {
                ui.header H2 {
                    +"Buy now"
                    noui.sub.header { +"\$10.99" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderClearingSegment() = example {
        ui.header H3 { +"Clearing" }

        p { +"A segment can clear floated content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderClearingSegment,
        ) {
            // <CodeBlock renderClearingSegment>
            ui.clearing.segment {
                ui.right.floated.button {
                    +"Floated"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloatedSegment() = example {
        ui.header H3 { +"Floated" }

        p { +"A segment can appear to the left or right of other content" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderFloatedSegment,
        ) {
            // <CodeBlock renderFloatedSegment>
            ui.right.floated.segment {
                +"This segment is right floated"
            }
            ui.left.floated.segment {
                +"This segment is left floated"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTextAlignmentSegment() = example {
        ui.header H3 { +"Text alignment" }

        p { +"A segment can have its text aligned to a side" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderTextAlignmentSegment,
        ) {
            // <CodeBlock renderTextAlignmentSegment>
            ui.right.aligned.segment {
                +"Right"
            }
            ui.center.aligned.segment {
                +"Center"
            }
            ui.left.aligned.segment {
                +"Left"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBasicSegment() = example {
        ui.header H3 { +"Basic" }

        p { +"A basic segment has no special formatting" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_segment_SegmentPage_kt_renderBasicSegment,
        ) {
            // <CodeBlock renderBasicSegment>
            ui.basic.segment {
                +LoremIpsum(30)
            }
            // </CodeBlock>
        }
    }
}
