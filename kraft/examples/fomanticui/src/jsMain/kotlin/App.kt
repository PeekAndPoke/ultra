package de.peekandpoke.kraft.examples.fomanticui

import de.peekandpoke.kraft.addons.routing.RouterComponent
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.App() = comp {
    App(it)
}

class App(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("unused")
    private val activeRoute by subscribingTo(router.current)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        console.info("rendering app ...")

        div("pusher") {
            div("full height") {
                div("toc") {
                    AppMenu()
                }
            }

            div("article") {
                RouterComponent(router = router)
            }
        }
    }
}
