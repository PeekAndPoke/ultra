package io.peekandpoke.funktor.demo.b2b2capp

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.B2b2cAppComponent() = comp {
    B2b2cAppComponent(it)
}

class B2b2cAppComponent(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        div(classes = "app") {
            RouterComponent()
        }
    }
}
