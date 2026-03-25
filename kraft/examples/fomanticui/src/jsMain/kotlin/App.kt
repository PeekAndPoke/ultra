package io.peekandpoke.kraft.examples.fomanticui

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.App() = comp {
    App(it)
}

class App(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {

        console.info("rendering app ...")

        div("pusher") {
            div("full height") {
                div("toc") {
                    AppMenu()
                }
            }

            div("article") {
                RouterComponent()
            }
        }
    }
}
