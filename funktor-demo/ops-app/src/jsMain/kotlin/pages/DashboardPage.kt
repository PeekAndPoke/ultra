package io.peekandpoke.funktor.demo.opsapp.pages

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
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.DashboardPage() = comp {
    DashboardPage(it)
}

class DashboardPage(ctx: NoProps) : PureComponent(ctx) {

    private val loader = dataLoader {
        Apis.orgs.list().map { it.data ?: emptyList() }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.tachometer_alternate()
                noui.content { +"Dashboard" }
            }
        }

        loader.renderDefault(this) { orgs ->
            ui.three.statistics {
                ui.statistic {
                    noui.value { +"${orgs.size}" }
                    noui.label { +"Organisations" }
                }
            }
        }
    }
}
