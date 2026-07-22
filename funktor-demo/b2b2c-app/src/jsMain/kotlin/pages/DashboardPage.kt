package io.peekandpoke.funktor.demo.b2b2capp.pages

import io.peekandpoke.funktor.demo.b2b2capp.State
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.DashboardPage() = comp {
    DashboardPage(it)
}

class DashboardPage(ctx: NoProps) : PureComponent(ctx) {

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.home()
                noui.content { +"Welcome" }
            }

            user?.let { u ->
                ui.list {
                    noui.item { +"Name: ${u.name}" }
                    noui.item { +"Email: ${u.email}" }
                }
            }

            // Populated once the client-side JWT decoder is wired (deferred); the selected org id
            // lives in the session permissions.
            auth.permissions.org?.let { org ->
                ui.info.message { +"Active organisation: $org" }
            }
        }
    }
}
