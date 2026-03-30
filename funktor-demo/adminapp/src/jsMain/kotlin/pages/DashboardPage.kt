package io.peekandpoke.funktor.demo.adminapp.pages

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.AttendeeModel
import io.peekandpoke.funktor.demo.common.funktorconf.EventModel
import io.peekandpoke.funktor.demo.common.funktorconf.SpeakerModel
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.combine
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

    data class State(
        val events: List<EventModel> = emptyList(),
        val speakers: List<SpeakerModel> = emptyList(),
        val attendees: List<AttendeeModel> = emptyList(),
    )

    private val loader = dataLoader {
        combine(
            Apis.funktorConf.listEvents(),
            Apis.funktorConf.listSpeakers(),
            Apis.funktorConf.listAttendees(),
        ) { events, speakers, attendees ->
            State(
                events = events.data ?: emptyList(),
                speakers = speakers.data ?: emptyList(),
                attendees = attendees.data ?: emptyList(),
            )
        }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.bullhorn()
                noui.content { +"FunktorConf Dashboard" }
            }
        }

        loader.renderDefault(this) { data ->
            renderStats(data)
            renderUpcomingEvents(data.events)
            renderRecentSpeakers(data.speakers)
        }
    }

    private fun FlowContent.renderStats(data: State) {
        ui.segment {
            ui.three.statistics {
                ui.statistic {
                    onClick { evt -> router.navToUri(evt, Nav.funktorConf.events()) }
                    noui.value { +"${data.events.size}" }
                    noui.label { +"Events" }
                }

                ui.statistic {
                    onClick { evt -> router.navToUri(evt, Nav.funktorConf.speakers()) }
                    noui.value { +"${data.speakers.size}" }
                    noui.label { +"Speakers" }
                }

                ui.statistic {
                    onClick { evt -> router.navToUri(evt, Nav.funktorConf.attendees()) }
                    noui.value { +"${data.attendees.size}" }
                    noui.label { +"Attendees" }
                }
            }
        }
    }

    private fun FlowContent.renderUpcomingEvents(events: List<EventModel>) {
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

    private fun FlowContent.renderRecentSpeakers(speakers: List<SpeakerModel>) {
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
