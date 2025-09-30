package de.peekandpoke.funktor.cluster

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.RenderFn
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent

class FunktorClusterOverviewPage(ctx: Ctx<Props>) : Component<FunktorClusterOverviewPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
        val customInternals: RenderFn,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Cluster") }

        ui.padded.segment {
            ui.header H2 { +"Funktor Server Internals" }

            props.customInternals(this)

            renderCards()
        }
    }

    private fun FlowContent.renderCards() {

        ui.dividing.header { +"Server and Global Locks" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href = props.ui.routes.locks.listServerBeacons()

                ui.center.aligned.content {
                    icon.large.font_awesome_flag()
                }
                ui.center.aligned.content {
                    noui.header { +"Server Beacons" }
                }
            }

            ui.card A {
                href = props.ui.routes.locks.listGlobalLocks()

                ui.center.aligned.content {
                    icon.large.unlock()
                }
                ui.center.aligned.content {
                    noui.header { +"Global Locks" }
                }
            }
        }

        ui.dividing.header { +"Workers and Background Jobs" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href = props.ui.routes.workers.list()

                ui.center.aligned.content {
                    icon.large.robot()
                }
                ui.center.aligned.content {
                    noui.header { +"Workers" }
                }
            }

            ui.card A {
                href = props.ui.routes.backgroundJobs.listQueued()

                ui.center.aligned.content {
                    icon.large.pallet()
                }
                ui.center.aligned.content {
                    noui.header { +"Background Jobs Queue" }
                }
            }

            ui.card A {
                href = props.ui.routes.backgroundJobs.listArchived()

                ui.center.aligned.content {
                    icon.large.boxes()
                }
                ui.center.aligned.content {
                    noui.header { +"Background Jobs Archive" }
                }
            }
        }

        ui.dividing.header { +"Storage" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href = props.ui.routes.vault.index()

                ui.center.aligned.content {
                    icon.large.database()
                }
                ui.center.aligned.content {
                    noui.header { +"Vault" }
                }
            }

            ui.card A {
                href = props.ui.routes.depot.listRepositories()

                ui.center.aligned.content {
                    icon.large.boxes()
                }
                ui.center.aligned.content {
                    noui.header { +"Depot" }
                }
            }

            ui.card A {
                href = props.ui.routes.storage.randomData.list()

                ui.center.aligned.content {
                    icon.large.database()
                }
                ui.center.aligned.content {
                    noui.header { +"Random Data" }
                }
            }

            ui.card A {
                href = props.ui.routes.storage.randomCache.list()

                ui.center.aligned.content {
                    icon.large.memory()
                }
                ui.center.aligned.content {
                    noui.header { +"Random Cache" }
                }
            }
        }
    }
}
