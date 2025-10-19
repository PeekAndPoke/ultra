package de.peekandpoke.funktor.demo.adminapp.pages

import de.peekandpoke.funktor.auth.widgets.ChangePasswordWidget
import de.peekandpoke.funktor.demo.adminapp.State
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.toasts
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
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
        ui.container {

            ui.cards {
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
}
