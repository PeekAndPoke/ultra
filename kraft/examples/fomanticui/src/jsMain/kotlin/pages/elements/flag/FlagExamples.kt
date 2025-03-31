@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.flag

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.semanticui.flag
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FlagExamples() = comp {
    FlagExamples(it)
}

class FlagExamples(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        example {

            HorizontalContentAndCode(
                ExtractedCodeBlocks.pages_elements_flag_FlagExamples_kt_renderSimpleExample,
            ) {
                // <CodeBlock renderSimpleExample>
                flag.germany()
                flag.japan()
                flag.indonesia()
                // </CodeBlock>
            }
        }
    }
}
