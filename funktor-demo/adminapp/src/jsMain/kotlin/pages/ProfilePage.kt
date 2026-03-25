package io.peekandpoke.funktor.demo.adminapp.pages

import io.peekandpoke.funktor.auth.widgets.ChangePasswordWidget
import io.peekandpoke.funktor.demo.adminapp.State
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.ProfilePage() = comp {
    ProfilePage(it)
}

class ProfilePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user!!

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.four.doubling.cards {
            noui.card {
                noui.content {
                    ui.header { +"Pofile" }

                    ui.list {
                        noui.item {
                            noui.header { +"Name" }
                            noui.description { +user.name }
                        }
                        noui.item {
                            noui.header { +"Email" }
                            noui.description { +user.email }
                        }
                    }
                }
            }

            noui.card {
                noui.content {
                    ui.header { +"Change Password" }

                    ChangePasswordWidget(State.auth) {
                        when (it) {
                            true -> toasts.info("Password changed successfully")
                            false -> toasts.error("Failed to change password")
                        }
                    }
                }
            }
        }
    }
}
