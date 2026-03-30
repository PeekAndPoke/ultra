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

            renderDevtools()
            renderAppIntrospection()
            renderLocks()
            renderWorkersAndJobs()
            renderStorage()
        }
    }

    private fun FlowContent.renderAppIntrospection() {
        ui.dividing.header { +"App Introspection" }

        ui.four.doubling.stackable.cards {
            ui.card A {
                href(props.ui.routes.app.endpoints())

                ui.center.aligned.content {
                    icon.large.sitemap()
                }
                ui.center.aligned.content {
                    noui.header { +"API Access Matrix" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.system())

                ui.center.aligned.content {
                    icon.large.server()
                }
                ui.center.aligned.content {
                    noui.header { +"System Info" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.config())

                ui.center.aligned.content {
                    icon.large.cog()
                }
                ui.center.aligned.content {
                    noui.header { +"Configuration" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.lifecycleHooks())

                ui.center.aligned.content {
                    icon.large.heartbeat()
                }
                ui.center.aligned.content {
                    noui.header { +"Lifecycle Hooks" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.cliCommands())

                ui.center.aligned.content {
                    icon.large.terminal()
                }
                ui.center.aligned.content {
                    noui.header { +"CLI Commands" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.authRealms())

                ui.center.aligned.content {
                    icon.large.shield_alternate()
                }
                ui.center.aligned.content {
                    noui.header { +"Auth Realms" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.fixtures())

                ui.center.aligned.content {
                    icon.large.puzzle_piece()
                }
                ui.center.aligned.content {
                    noui.header { +"Fixtures" }
                }
            }

            ui.card A {
                href(props.ui.routes.app.repairs())

                ui.center.aligned.content {
                    icon.large.wrench()
                }
                ui.center.aligned.content {
                    noui.header { +"Repairs" }
                }
            }
        }
    }

    private fun FlowContent.renderDevtools() {
        ui.dividing.header { +"Dev Tools" }

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

    private fun FlowContent.renderLocks() {
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
    }

    private fun FlowContent.renderWorkersAndJobs() {
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
}
