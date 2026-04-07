package io.peekandpoke.funktor.demo.adminapp.pages

import io.peekandpoke.funktor.auth.widgets.ChangePasswordWidget
import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.State
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.funktor.rest.acl.UserApiAccessMatrix
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

@Suppress("FunctionName")
fun Tag.ProfilePage() = comp {
    ProfilePage(it)
}

class ProfilePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth by subscribingTo(State.auth)
    private val user get() = auth.user!!

    private val aclLoader = dataLoader {
        Apis.auth.getMyApiAccess().map { it.data ?: UserApiAccessMatrix(entries = emptyList()) }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.user()
                noui.content { +"Profile" }
            }
        }

        ui.two.doubling.cards {
            noui.card {
                noui.content {
                    ui.header { +"User Info" }

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

        ui.segment {
            ui.header H2 {
                icon.lock()
                noui.content { +"API Access" }
            }

            aclLoader.renderDefault(this) { matrix ->
                renderAccessMatrix(matrix)
            }
        }
    }

    private fun FlowContent.renderAccessMatrix(matrix: UserApiAccessMatrix) {
        if (matrix.entries.isEmpty()) {
            ui.message { +"No accessible endpoints." }
            return
        }

        val grouped = matrix.entries
            .sortedBy { it.uri }
            .groupBy { entry -> entry.uri.split("/").drop(1).firstOrNull() ?: "" }

        grouped.forEach { (group, entries) ->
            ui.header H3 { +"/$group" }

            ui.compact.striped.table Table {
                thead {
                    tr {
                        th { +"Method" }
                        th { +"Endpoint" }
                        th { +"Access" }
                    }
                }
                tbody {
                    entries.forEach { entry ->
                        tr {
                            td {
                                when (entry.method) {
                                    "GET" -> ui.blue.label { +entry.method }
                                    "POST" -> ui.green.label { +entry.method }
                                    "PUT" -> ui.orange.label { +entry.method }
                                    "DELETE" -> ui.red.label { +entry.method }
                                    else -> ui.label { +entry.method }
                                }
                            }
                            td { +entry.uri }
                            td {
                                when (entry.level.name) {
                                    "Granted" -> ui.green.label { icon.check_circle(); +"Granted" }
                                    "Partial" -> ui.yellow.label { icon.exclamation_circle(); +"Partial" }
                                    else -> ui.grey.label { +entry.level.name }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
