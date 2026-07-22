package io.peekandpoke.funktor.demo.b2b2capp.layout

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Color
import kotlinx.css.backgroundColor
import kotlinx.css.minHeight
import kotlinx.css.paddingTop
import kotlinx.css.px
import kotlinx.css.vh
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

/**
 * A dedicated logged-out (auth) layout for the b2b2c end-user app — wraps the shared
 * [io.peekandpoke.funktor.auth.pages.AuthLogin] widget in the app's own branded chrome, visually
 * distinct from the b2b admin console (violet vs teal).
 */
@Suppress("FunctionName")
fun Tag.LoggedOutLayout(
    content: FlowContent.() -> Unit,
) = comp(
    LoggedOutLayout.Props(content = content)
) {
    LoggedOutLayout(it)
}

class LoggedOutLayout(ctx: Ctx<Props>) : Component<LoggedOutLayout.Props>(ctx) {

    data class Props(
        val content: FlowContent.() -> Unit,
    )

    override fun VDom.render() {
        div {
            css {
                minHeight = 100.vh
                paddingTop = 60.px
                backgroundColor = Color("#5b21b6")
            }

            ui.text.container {
                ui.raised.segment {
                    ui.violet.inverted.padded.segment {
                        ui.header { +"Funktor B2B2C" }
                        +"End-user portal"
                    }

                    ui.padded.segment {
                        props.content(this)
                    }
                }
            }
        }
    }
}
