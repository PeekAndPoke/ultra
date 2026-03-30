package io.peekandpoke.funktor.demo.adminapp.pages

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.AttendeeModel
import io.peekandpoke.funktor.demo.common.funktorconf.EventModel
import io.peekandpoke.funktor.demo.common.funktorconf.SpeakerModel
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
fun Tag.DashboardPage() = comp {
    DashboardPage(it)
}

class DashboardPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private var events: List<EventModel> by value(emptyList())
    private var speakers: List<SpeakerModel> by value(emptyList())
    private var attendees: List<AttendeeModel> by value(emptyList())

    //  INIT  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        launch { events = Apis.funktorConf.listEvents().first().data ?: emptyList() }
        launch { speakers = Apis.funktorConf.listSpeakers().first().data ?: emptyList() }
        launch { attendees = Apis.funktorConf.listAttendees().first().data ?: emptyList() }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.bullhorn()
                noui.content { +"FunktorConf Dashboard" }
            }
        }

        renderStats()
        renderUpcomingEvents()
        renderRecentSpeakers()
    }

    private fun FlowContent.renderStats() {
        ui.three.statistics {
            ui.statistic {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.events()) }
                noui.value { +"${events.size}" }
                noui.label { +"Events" }
            }

            ui.statistic {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.speakers()) }
                noui.value { +"${speakers.size}" }
                noui.label { +"Speakers" }
            }

            ui.statistic {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.attendees()) }
                noui.value { +"${attendees.size}" }
                noui.label { +"Attendees" }
            }
        }
    }

    private fun FlowContent.renderUpcomingEvents() {
        ui.segment {
            ui.header H2 {
                icon.calendar()
                noui.content { +"Events" }
            }

            if (events.isEmpty()) {
                ui.message { +"No events yet." }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Venue" }
                            th { +"Dates" }
                            th { +"Status" }
                        }
                    }
                    tbody {
                        events.forEach { event ->
                            tr {
                                td {
                                    ui.link {
                                        onClick { evt ->
                                            router.navToUri(evt, Nav.funktorConf.eventEdit(event.id))
                                        }
                                        +event.name
                                    }
                                }
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
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderRecentSpeakers() {
        ui.segment {
            ui.header H2 {
                icon.microphone()
                noui.content { +"Speakers" }
            }

            if (speakers.isEmpty()) {
                ui.message { +"No speakers yet." }
            } else {
                ui.four.stackable.cards {
                    speakers.forEach { speaker ->
                        ui.card {
                            onClick { evt ->
                                router.navToUri(evt, Nav.funktorConf.speakerEdit(speaker.id))
                            }

                            noui.content {
                                ui.header { +speaker.name }
                                noui.meta { +speaker.talkTitle }
                                noui.description {
                                    val bio = speaker.bio.take(100)
                                    +(if (speaker.bio.length > 100) "$bio..." else bio)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
