package io.peekandpoke.funktor.demo.opsapp.layout

import io.peekandpoke.funktor.demo.opsapp.Nav
import io.peekandpoke.funktor.demo.opsapp.State
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.css.marginLeft
import kotlinx.css.marginRight
import kotlinx.css.marginTop
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.LoggedInLayout(
    content: FlowContent.() -> Unit,
) = comp(
    LoggedInLayout.Props(content = content)
) {
    LoggedInLayout(it)
}

class LoggedInLayout(ctx: Ctx<Props>) : Component<LoggedInLayout.Props>(ctx) {

    data class Props(
        val content: FlowContent.() -> Unit,
    )

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user

    override fun VDom.render() {
        renderMenu()

        div {
            css {
                marginTop = 16.px
                marginLeft = 250.px
                marginRight = 50.px
            }

            props.content(this)
        }
    }

    private fun FlowContent.renderMenu() {
        user ?: return

        ui.inverted.sidebar.vertical.visible.menu {
            noui.item {
                +"Funktor Ops"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.dashboard()) }
                icon.tachometer_alternate()
                +"Dashboard"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.orgs()) }
                icon.sitemap()
                +"Organisations"
            }

            noui.item()

            noui.item A {
                onClick { evt ->
                    router.navToUri(evt, Nav.auth.login())
                    State.auth.logout()
                }
                icon.sign_out_alternate()
                +"Logout"
            }
        }
    }
}
