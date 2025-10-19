package de.peekandpoke.funktor.demo.adminapp.layout

import de.peekandpoke.funktor.demo.adminapp.Nav
import de.peekandpoke.funktor.demo.adminapp.State
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.router
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.css.marginLeft
import kotlinx.css.marginRight
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.LoggedInLayout(
    inner: FlowContent.() -> Unit,
) = comp(
    LoggedInLayout.Props(inner = inner)
) {
    LoggedInLayout(it)
}

class LoggedInLayout(ctx: Ctx<Props>) : Component<LoggedInLayout.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val inner: FlowContent.() -> Unit,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        val u = user ?: return

        ui.sidebar.vertical.visible.menu {
            ui.basic.segment {
                onClick { evt -> router.navToUri(evt, Nav.profile()) }
                +u.name
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.dashboard()) }
                +"Dashboard"
            }

            noui.item A {
                onClick { evt ->
                    router.navToUri(evt, Nav.login())
                    State.auth.logout()
                }
                icon.sign_out_alternate()
                +"Logout"
            }
        }

        div {
            css {
                marginLeft = 250.px
                marginRight = 50.px
            }

            props.inner(this)
        }
    }
}
