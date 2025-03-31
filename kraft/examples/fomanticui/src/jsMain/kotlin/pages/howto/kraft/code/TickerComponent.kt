@file:Suppress("FunctionName")

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.streams.addons.ticker
import de.peekandpoke.kraft.vdom.VDom
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
