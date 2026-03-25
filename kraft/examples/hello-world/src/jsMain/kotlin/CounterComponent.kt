package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.CounterComponent(start: Int) = comp(
    CounterComponent.Props(start = start)
) {
    CounterComponent(it)
}

class CounterComponent(ctx: Ctx<Props>) : Component<CounterComponent.Props>(ctx) {

    data class Props(
        val start: Int,
    )

    private var counter: Int by value(props.start)

    override fun VDom.render() {
        div {
            div { +"Value: $counter" }
            div {
                button { onClick { counter-- }; +"Minus" }
                button { onClick { counter++ }; +"Plus" }
            }
        }
    }
}
