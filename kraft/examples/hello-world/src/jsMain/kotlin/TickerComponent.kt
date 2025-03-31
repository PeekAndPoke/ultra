package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.streams.addons.ticker
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.TickerComponent(
    delay: Int,
) = comp(
    TickerComponent.Props(delay = delay)
) {
    TickerComponent(it)
}

class TickerComponent(ctx: Ctx<Props>) : Component<TickerComponent.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val delay: Int,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val tick = ticker(props.delay)

    private val currentTick: Long by subscribingTo(tick)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div {
            +"Ticker: $currentTick"
        }
    }
}
