package io.peekandpoke.funktor.demo.adminapp.pages.funktorconf

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.EventModel
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

@Suppress("FunctionName")
fun Tag.EventsListPage() = comp {
    EventsListPage(it)
}

class EventsListPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        Apis.funktorConf.listEvents().map { it.data ?: emptyList() }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.calendar()
                noui.content { +"Events" }
            }

            ui.green.button {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.eventEdit("_new_")) }
                icon.plus()
                +"New Event"
            }
        }

        loader.renderDefault(this) { events ->
            renderEventsTable(events)
        }
    }

    private fun FlowContent.renderEventsTable(events: List<EventModel>) {
        ui.segment {
            if (events.isEmpty()) {
                ui.message { +"No events found. Create one to get started." }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Venue" }
                            th { +"Dates" }
                            th { +"Status" }
                            th { +"Actions" }
                        }
                    }
                    tbody {
                        events.forEach { event ->
                            tr {
                                td { +event.name }
                                td { +event.venue }
                                td { +"${event.startDate} — ${event.endDate}" }
                                td {
                                    when (event.status.name) {
                                        "Published" -> ui.green.label { +"Published" }
                                        "Draft" -> ui.yellow.label { +"Draft" }
                                        "Archived" -> ui.grey.label { +"Archived" }
                                        else -> ui.label { +event.status.name }
                                    }
                                }
                                td {
                                    ui.small.blue.button {
                                        onClick { evt ->
                                            router.navToUri(evt, Nav.funktorConf.eventEdit(event.id))
                                        }
                                        icon.edit()
                                        +"Edit"
                                    }

                                    ui.small.red.button {
                                        onClick {
                                            launch {
                                                try {
                                                    val response = Apis.funktorConf.deleteEvent(event.id).first()
                                                    if (response.isSuccess()) {
                                                        toasts.info("Event deleted")
                                                        loader.reload()
                                                    } else {
                                                        toasts.error("Failed to delete event")
                                                    }
                                                } catch (e: Exception) {
                                                    toasts.error("Failed to delete event: ${e.message}")
                                                }
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
