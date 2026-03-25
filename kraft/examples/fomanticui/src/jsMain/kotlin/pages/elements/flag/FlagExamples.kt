@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.elements.flag

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.flag
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
