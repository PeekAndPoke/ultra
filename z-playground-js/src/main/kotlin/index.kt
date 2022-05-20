import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.stream.createHTML

fun main() {

    createHTML().body {
        val x = +"==========================================================="

        div {
            div { +"THIS IS A DIV IN A DIV" }
        }

        val y = +"==========================================================="
    }

    createHTML().body {
        ui.basic.segment {
            ui.list {
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
                ui.item Label { +"TEXT" }
            }
        }

        ui.basic.segment {
            ui.list {
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
                icon.large.red.wrench()
            }
        }

        ui.segment {
            ui.header H3 { +"Colors" }

            ui.eight.column.grid {
                ui.center.aligned.column {
                    icon.large.red.couch()
                    div { +"red" }
                }
                ui.center.aligned.column {
                    icon.large.orange.couch()
                    noui { +"orange" }
                }
                ui.center.aligned.column {
                    icon.large.yellow.couch()
                    noui { +"yellow" }
                }
                ui.center.aligned.column {
                    icon.large.olive.couch()
                    noui { +"olive" }
                }
                ui.center.aligned.column {
                    icon.large.green.couch()
                    noui { +"green" }
                }
                ui.center.aligned.column {
                    icon.large.teal.couch()
                    noui { +"teal" }
                }
                ui.center.aligned.column {
                    icon.large.blue.couch()
                    noui { +"blue" }
                }
                ui.center.aligned.column {
                    icon.large.violet.couch()
                    noui { +"violet" }
                }
                ui.center.aligned.column {
                    icon.large.purple.couch()
                    noui { +"purple" }
                }
                ui.center.aligned.column {
                    icon.large.pink.couch()
                    noui { +"pink" }
                }
                ui.center.aligned.column {
                    icon.large.brown.couch()
                    noui { +"brown" }
                }
                ui.center.aligned.column {
                    icon.large.grey.couch()
                    noui { +"grey" }
                }
                ui.center.aligned.column {
                    icon.large.black.couch()
                    noui { +"black" }
                }
                ui.center.aligned.column {
                    icon.large.white.couch()
                    noui { +"white" }
                }
                ui.center.aligned.inverted.blue.column {
                    icon.large.white.couch()
                    noui { +"white" }
                }
            }
        }
    }
}

