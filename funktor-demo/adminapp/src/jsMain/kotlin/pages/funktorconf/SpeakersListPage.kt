package io.peekandpoke.funktor.demo.adminapp.pages.funktorconf

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.SpeakerModel
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
fun Tag.SpeakersListPage() = comp {
    SpeakersListPage(it)
}

class SpeakersListPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        Apis.funktorConf.listSpeakers().map { it.data ?: emptyList() }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.microphone()
                noui.content { +"Speakers" }
            }

            ui.green.button {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.speakerEdit("_new_")) }
                icon.plus()
                +"New Speaker"
            }
        }

        loader.renderDefault(this) { speakers ->
            renderSpeakersTable(speakers)
        }
    }

    private fun FlowContent.renderSpeakersTable(speakers: List<SpeakerModel>) {
        ui.segment {
            if (speakers.isEmpty()) {
                ui.message { +"No speakers found. Add one to get started." }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Talk Title" }
                            th { +"Bio" }
                            th { +"Actions" }
                        }
                    }
                    tbody {
                        speakers.forEach { speaker ->
                            tr {
                                td { +speaker.name }
                                td { +speaker.talkTitle }
                                td {
                                    val bio = speaker.bio.take(80)
                                    +(if (speaker.bio.length > 80) "$bio..." else bio)
                                }
                                td {
                                    ui.small.blue.button {
                                        onClick { evt ->
                                            router.navToUri(evt, Nav.funktorConf.speakerEdit(speaker.id))
                                        }
                                        icon.edit()
                                        +"Edit"
                                    }

                                    ui.small.red.button {
                                        onClick {
                                            launch {
                                                try {
                                                    val response = Apis.funktorConf.deleteSpeaker(speaker.id).first()
                                                    if (response.isSuccess()) {
                                                        toasts.info("Speaker deleted")
                                                        loader.reload()
                                                    } else {
                                                        toasts.error("Failed to delete speaker")
                                                    }
                                                } catch (e: Exception) {
                                                    toasts.error("Failed to delete speaker: ${e.message}")
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
