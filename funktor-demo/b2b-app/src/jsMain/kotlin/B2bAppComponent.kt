package io.peekandpoke.funktor.demo.b2bapp

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.B2bAppComponent() = comp {
    B2bAppComponent(it)
}

class B2bAppComponent(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        div(classes = "app") {
            RouterComponent()
        }
    }
}
