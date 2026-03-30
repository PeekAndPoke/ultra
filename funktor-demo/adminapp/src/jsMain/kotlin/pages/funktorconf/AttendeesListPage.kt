package io.peekandpoke.funktor.demo.adminapp.pages.funktorconf

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.AttendeeModel
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

@Suppress("FunctionName")
fun Tag.AttendeesListPage() = comp {
    AttendeesListPage(it)
}

class AttendeesListPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private var attendees: List<AttendeeModel> by value(emptyList())

    //  INIT  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        loadAttendees()
    }

    private fun loadAttendees() {
        launch {
            attendees = Apis.funktorConf.listAttendees().first().data ?: emptyList()
        }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.users()
                noui.content { +"Attendees" }
            }

            ui.green.button {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.attendeeEdit("_new_")) }
                icon.plus()
                +"New Attendee"
            }
        }

        renderAttendeesTable()
    }

    private fun FlowContent.renderAttendeesTable() {
        ui.segment {
            if (attendees.isEmpty()) {
                ui.message { +"No attendees found. Register one to get started." }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Email" }
                            th { +"Ticket" }
                            th { +"Checked In" }
                            th { +"Actions" }
                        }
                    }
                    tbody {
                        attendees.forEach { attendee ->
                            tr {
                                td { +attendee.name }
                                td { +attendee.email }
                                td {
                                    when (attendee.ticketType.name) {
                                        "Vip" -> ui.purple.label { +"VIP" }
                                        "Speaker" -> ui.blue.label { +"Speaker" }
                                        "Staff" -> ui.orange.label { +"Staff" }
                                        else -> ui.label { +"Standard" }
                                    }
                                }
                                td {
                                    if (attendee.checkedIn) {
                                        icon.green.check_circle()
                                        +"Yes"
                                    } else {
                                        icon.red.times_circle()
                                        +"No"
                                    }
                                }
                                td {
                                    ui.small.blue.button {
                                        onClick { evt ->
                                            router.navToUri(evt, Nav.funktorConf.attendeeEdit(attendee.id))
                                        }
                                        icon.edit()
                                        +"Edit"
                                    }

                                    ui.small.red.button {
                                        onClick {
                                            launch {
                                                Apis.funktorConf.deleteAttendee(attendee.id).first()
                                                loadAttendees()
                                            }
                                        }
                                        icon.trash()
                                        +"Delete"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
