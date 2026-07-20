package io.peekandpoke.funktor.demo.b2bapp.layout

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
 * A dedicated logged-out (auth) layout for the b2b app — a proof of concept of the full-control
 * composable path: instead of the default fullscreen-background `LoginPage`, the app wraps the
 * shared [io.peekandpoke.funktor.auth.pages.AuthLogin] widget in its own branded chrome.
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
                backgroundColor = Color("#0e7490")
            }

            ui.text.container {
                ui.raised.segment {
                    ui.teal.inverted.padded.segment {
                        ui.header { +"Funktor B2B" }
                        +"Customer administration console"
                    }

                    ui.padded.segment {
                        props.content(this)
                    }
                }
            }
        }
    }
}
