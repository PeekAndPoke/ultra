package io.peekandpoke.funktor.demo.b2bapp.layout

import io.peekandpoke.funktor.demo.b2bapp.Nav
import io.peekandpoke.funktor.demo.b2bapp.State
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

    /**
     * The selected organisation's display NAME.
     *
     * From the sign-in response's `AuthOrgRef`, not from the token: the JWT carries only the org's
     * id, and its `_key` is a generated Arango key (e.g. `2059721`) — not something to show a user.
     * Null on an org-less session, which for b2b means the org picker has not completed.
     */
    private val orgName: String? get() = auth.org?.name

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
                ui.inverted.header {
                    // Falls back rather than hiding: an org-less b2b session is an anomaly worth
                    // seeing in the chrome, not something to render as a blank header.
                    +(orgName ?: "No organisation")
                    noui.sub.header { +"Funktor B2B" }
                }
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.dashboard()) }
                icon.home()
                +"Dashboard"
            }

            noui.item A {
                onClick { evt -> router.navToUri(evt, Nav.members()) }
                icon.users()
                +"Members"
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
