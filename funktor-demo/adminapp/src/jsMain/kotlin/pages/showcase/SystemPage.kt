package io.peekandpoke.funktor.demo.adminapp.pages.showcase

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.common.showcase.AppLifecycleInfo
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
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
fun Tag.SystemPage() = comp {
    SystemPage(it)
}

class SystemPage(ctx: NoProps) : PureComponent(ctx) {

    private var lifecycleInfo: AppLifecycleInfo? by value(null)

    init {
        launch { lifecycleInfo = Apis.showcase.getAppLifecycle().first().data }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 { +"System" }
            ui.dividing.header H3 { +"Application lifecycle and runtime information" }
        }

        renderLifecycleInfo()
    }

    private fun FlowContent.renderLifecycleInfo() {
        ui.segment {
            ui.header H2 { icon.server(); noui.content { +"App Lifecycle" } }

            lifecycleInfo?.let { info ->
                ui.four.statistics {
                    ui.statistic { noui.value { +info.startedAt.take(19) }; noui.label { +"Started At" } }
                    ui.statistic { noui.value { +"${info.uptimeSeconds}s" }; noui.label { +"Uptime" } }
                    ui.statistic { noui.value { +info.jvmVersion }; noui.label { +"JVM" } }
                    ui.statistic { noui.value { +info.kotlinVersion }; noui.label { +"Kotlin" } }
                }

                ui.divider()

                ui.striped.table Table {
                    thead { tr { th { +"Property" }; th { +"Value" } } }
                    tbody {
                        tr { td { +"Started At" }; td { +info.startedAt } }
                        tr { td { +"Uptime" }; td { +"${info.uptimeSeconds} seconds" } }
                        tr { td { +"JVM Version" }; td { +info.jvmVersion } }
                        tr { td { +"Kotlin Version" }; td { +info.kotlinVersion } }
                    }
                }
            } ?: ui.placeholder.segment { ui.header { +"Loading..." } }
        }
    }
}
