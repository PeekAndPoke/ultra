package io.peekandpoke.funktor.inspect

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent

class FunktorInspectOverviewPage(ctx: Ctx<Props>) : Component<FunktorInspectOverviewPage.Props>(ctx) {

    data class Props(
        val ui: FunktorInspectUi,
    )

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Inspect") }

        ui.padded.segment {
            ui.header H2 { +"Funktor Inspect" }

            renderLogging()
            renderDevtools()
            renderCluster()
            renderStorage()
            renderAppIntrospection()
        }
    }

    private fun FlowContent.renderLogging() {
        ui.dividing.header { +"Logging" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href(props.ui.routes.logging.list())

                ui.center.aligned.content {
                    icon.large.list_alternate()
                }
                ui.center.aligned.content {
                    noui.header { +"Log Entries" }
                }
            }
        }
    }

    private fun FlowContent.renderDevtools() {
        ui.dividing.header { +"Devtools" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href(props.ui.routes.cluster.devtools.requestHistory())

                ui.center.aligned.content {
                    icon.large.cloud_upload_alternate()
                }
                ui.center.aligned.content {
                    noui.header { +"Request History" }
                }
            }
        }
    }

    private fun FlowContent.renderCluster() {
        ui.dividing.header { +"Server and Global Locks" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href(props.ui.routes.cluster.locks.listServerBeacons())

                ui.center.aligned.content {
                    icon.large.font_awesome_flag()
                }
                ui.center.aligned.content {
                    noui.header { +"Server Beacons" }
                }
            }

            ui.card A {
                href(props.ui.routes.cluster.locks.listGlobalLocks())

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
                href(props.ui.routes.cluster.workers.list())

                ui.center.aligned.content {
                    icon.large.robot()
                }
                ui.center.aligned.content {
                    noui.header { +"Workers" }
                }
            }

            ui.card A {
                href(props.ui.routes.cluster.backgroundJobs.listQueued())

                ui.center.aligned.content {
                    icon.large.pallet()
                }
                ui.center.aligned.content {
                    noui.header { +"Background Jobs Queue" }
                }
            }

            ui.card A {
                href(props.ui.routes.cluster.backgroundJobs.listArchived())

                ui.center.aligned.content {
                    icon.large.boxes()
                }
                ui.center.aligned.content {
                    noui.header { +"Background Jobs Archive" }
                }
            }
        }
    }

    private fun FlowContent.renderStorage() {
        ui.dividing.header { +"Storage" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href(props.ui.routes.cluster.vault.index())

                ui.center.aligned.content {
                    icon.large.database()
                }
                ui.center.aligned.content {
                    noui.header { +"Vault" }
                }
            }

            ui.card A {
                href(props.ui.routes.cluster.depot.listRepositories())

                ui.center.aligned.content {
                    icon.large.boxes()
                }
                ui.center.aligned.content {
                    noui.header { +"Depot" }
                }
            }

            ui.card A {
                href(props.ui.routes.cluster.storage.randomData.list())

                ui.center.aligned.content {
                    icon.large.database()
                }
                ui.center.aligned.content {
                    noui.header { +"Random Data" }
                }
            }

            ui.card A {
                href(props.ui.routes.cluster.storage.randomCache.list())

                ui.center.aligned.content {
                    icon.large.memory()
                }
                ui.center.aligned.content {
                    noui.header { +"Random Cache" }
                }
            }
        }
    }

    private fun FlowContent.renderAppIntrospection() {
        ui.dividing.header { +"App Introspection" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href(props.ui.routes.app.overview())

                ui.center.aligned.content {
                    icon.large.search()
                }
                ui.center.aligned.content {
                    noui.header { +"App Overview" }
                }
            }
        }
    }
}
