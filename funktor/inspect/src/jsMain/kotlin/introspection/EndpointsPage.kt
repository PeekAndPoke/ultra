package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.funktor.cluster.renderDefault
import io.peekandpoke.funktor.inspect.introspection.api.EndpointInfo
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class EndpointsPage(ctx: Ctx<Props>) : Component<EndpointsPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: IntrospectionUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        props.ui.api
            .getAllEndpoints()
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Endpoints") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Endpoints" }
            }

            loader.renderDefault(this) { data ->
                renderTable(data)
            }
        }
    }

    private fun FlowContent.renderTable(endpoints: List<EndpointInfo>) {
        ui.striped.selectable.table Table {
            thead {
                tr {
                    th { +"Feature" }
                    th { +"Group" }
                    th { +"Method" }
                    th { +"URI" }
                    th { +"Auth" }
                }
            }
            tbody {
                endpoints.forEach { endpoint ->
                    tr {
                        td { +endpoint.feature }
                        td { +endpoint.group }
                        td { +endpoint.method }
                        td { +endpoint.uri }
                        td { +endpoint.authDescription }
                    }
                }
            }
        }
    }
}
