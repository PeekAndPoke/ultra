package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.funktor.cluster.renderDefault
import io.peekandpoke.funktor.inspect.introspection.api.AppLifecycleInfo
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div

class SystemInfoPage(ctx: Ctx<Props>) : Component<SystemInfoPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: IntrospectionUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        props.ui.api
            .getAppLifecycle()
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "System Info") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"System Info" }
            }

            loader.renderDefault(this) { data ->
                renderStats(data)
            }
        }
    }

    private fun FlowContent.renderStats(info: AppLifecycleInfo) {
        ui.four.doubling.stackable.statistics {
            ui.statistic {
                noui.value { +info.startedAt }
                noui.label { +"Started At" }
            }

            ui.statistic {
                noui.value { +"${info.uptimeSeconds}s" }
                noui.label { +"Uptime" }
            }

            ui.statistic {
                noui.value { +info.jvmVersion }
                noui.label { +"JVM Version" }
            }

            ui.statistic {
                noui.value { +info.kotlinVersion }
                noui.label { +"Kotlin Version" }
            }
        }
    }
}
