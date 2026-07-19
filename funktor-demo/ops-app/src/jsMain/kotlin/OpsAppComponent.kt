package io.peekandpoke.funktor.demo.opsapp

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.OpsAppComponent() = comp {
    OpsAppComponent(it)
}

class OpsAppComponent(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        div(classes = "app") {
            RouterComponent()
        }
    }
}
