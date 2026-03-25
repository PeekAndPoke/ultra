@file:Suppress("FunctionName")

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.ops.ticker
import kotlinx.html.Tag
import kotlinx.html.div

fun Tag.TickerComponent(delay: Int) = comp(
    TickerComponent.Props(delay = delay)
) {
    TickerComponent(it)
}

class TickerComponent(ctx: Ctx<Props>) : Component<TickerComponent.Props>(ctx) {

    data class Props(
        val delay: Int,
    )

    /**
     * Here we define a stream.
     * The stream could also be passed in by the props or could be a global variable somewhere or similar.
     */
    private val stream = ticker(props.delay)

    /**
     * Here we define a property that subscribes to the stream, triggering a redraw whenever a new value
     * is published by the stream.
     */
    private val counter by subscribingTo(stream)

    override fun VDom.render() {
        div {
            +"Ticker: $counter"
        }
    }
}
// </CodeBlock>
