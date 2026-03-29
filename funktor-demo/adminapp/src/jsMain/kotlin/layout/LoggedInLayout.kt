package io.peekandpoke.funktor.demo.adminapp.layout

import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.adminapp.State
import io.peekandpoke.funktor.demo.adminapp.funktorInspect
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
import kotlinx.css.Color
import kotlinx.css.FontWeight
import kotlinx.css.color
import kotlinx.css.fontSize
import kotlinx.css.fontWeight
import kotlinx.css.marginLeft
import kotlinx.css.marginRight
import kotlinx.css.marginTop
import kotlinx.css.opacity
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
                onClick { evt -> router.navToUri(evt, Nav.dashboard()) }
                icon.home()
                +"Dashboard"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.profile()) }
                icon.user()
                +"Profile"
            }

            renderMenuHeader("Showcase")

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.showcase.core()) }
                icon.cogs()
                +"Core Features"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.showcase.rest()) }
                icon.code()
                +"REST API Explorer"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.showcase.auth()) }
                icon.shield_alternate()
                +"Auth & Authorization"
            }

            renderMenuHeader("Cluster")

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.showcase.cluster()) }
                icon.cubes()
                +"Cluster Features"
            }

            renderMenuHeader("Messaging & Real-Time")

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.showcase.messaging()) }
                icon.envelope()
                +"Messaging"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.showcase.sse()) }
                icon.bolt()
                +"SSE Demo"
            }

            if (auth.user?.isSuperUser == true) {
                renderMenuHeader("Admin Tools")

                noui.item A {
                    onClick { evt -> router.navToUri(evt, funktorInspect.routes.overview()) }
                    icon.search()
                    +"Funktor Inspect"
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

    private fun FlowContent.renderMenuHeader(title: String) {
        noui.item {
            css {
                opacity = 0.7
                fontSize = 11.px
                fontWeight = FontWeight.bold
                color = Color("#aaa")
            }
            +title.uppercase()
        }
    }
}
