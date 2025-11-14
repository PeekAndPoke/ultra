package de.peekandpoke.funktor.demo.adminapp.layout

import de.peekandpoke.funktor.demo.adminapp.Nav
import de.peekandpoke.funktor.demo.adminapp.State
import de.peekandpoke.funktor.demo.adminapp.funktorCluster
import de.peekandpoke.funktor.demo.adminapp.funktorLogging
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
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
