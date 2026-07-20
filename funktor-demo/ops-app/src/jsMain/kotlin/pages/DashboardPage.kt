package io.peekandpoke.funktor.demo.opsapp.pages

import io.peekandpoke.funktor.demo.common.operator.OperatorDashboardStats
import io.peekandpoke.funktor.demo.opsapp.Apis
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.DashboardPage() = comp {
    DashboardPage(it)
}

class DashboardPage(ctx: NoProps) : PureComponent(ctx) {

    private val loader = dataLoader {
        Apis.operator.getDashboardStats().map { it.data }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.tachometer_alternate()
                noui.content { +"Dashboard" }
            }
        }

        loader.renderDefault(this) { stats ->
            stats ?: return@renderDefault

            renderStats(stats)
        }
    }

    private fun FlowContent.renderStats(stats: OperatorDashboardStats) {
        ui.three.statistics {
            ui.statistic {
                noui.value { +"${stats.orgs}" }
                noui.label { +"Organisations" }
            }
            ui.statistic {
                noui.value { +"${stats.branches}" }
                noui.label { +"Branches" }
            }
            ui.statistic {
                noui.value { +"${stats.operators}" }
                noui.label { +"Operators" }
            }
        }

        ui.segment {
            ui.header H3 { +"Organisations by status" }

            ui.three.statistics {
                ui.green.statistic {
                    noui.value { +"${stats.activeOrgs}" }
                    noui.label { +"Active" }
                }
                ui.yellow.statistic {
                    noui.value { +"${stats.suspendedOrgs}" }
                    noui.label { +"Suspended" }
                }
                ui.grey.statistic {
                    noui.value { +"${stats.archivedOrgs}" }
                    noui.label { +"Archived" }
                }
            }
        }
    }
}
