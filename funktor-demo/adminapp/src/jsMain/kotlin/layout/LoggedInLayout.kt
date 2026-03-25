package io.peekandpoke.funktor.demo.adminapp.layout

import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.adminapp.State
import io.peekandpoke.funktor.demo.adminapp.funktorCluster
import io.peekandpoke.funktor.demo.adminapp.funktorLogging
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

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val content: FlowContent.() -> Unit,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

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
        val u = user ?: return

        ui.inverted.sidebar.vertical.visible.menu {
            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.profile()) }
                +"Profile"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.dashboard()) }
                +"Dashboard"
            }

            if (auth.user?.isSuperUser == true) {
                noui.item()

                noui.item A {
                    onClick { evt -> router.navToUri(evt, funktorLogging.routes.list()) }
                    +"Server Logs"
                }
                noui.item A {
                    onClick { evt -> router.navToUri(evt, funktorCluster.routes.overview()) }
                    +"Server Internals"
                }
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
