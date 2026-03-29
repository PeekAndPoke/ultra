package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.funktor.cluster.renderDefault
import io.peekandpoke.funktor.inspect.introspection.api.ApiAccessMatrixModel
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.css.Display
import kotlinx.css.Position
import kotlinx.css.WhiteSpace
import kotlinx.css.display
import kotlinx.css.position
import kotlinx.css.px
import kotlinx.css.top
import kotlinx.css.whiteSpace
import kotlinx.html.FlowContent
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.summary
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
            .getApiAccessMatrix()
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "API Access Matrix") }

        ui.basic.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"API Access Matrix" }
            }

            loader.renderDefault(this) { data ->

                data.numEndpointsAllowingSensitiveData.takeIf { it > 0 }?.let { count ->
                    ui.blue.message {
                        icon.user_secret()
                        +"There are $count endpoints allowing sensitive data exposure!"
                    }
                }

                data.numEndpointsWithErrors.takeIf { it > 0 }?.let { count ->
                    ui.red.message {
                        icon.exclamation()
                        +"There are $count endpoints having problems!"
                    }
                }

                renderMatrix(data)
            }
        }
    }

    private fun FlowContent.renderMatrix(data: ApiAccessMatrixModel) {

        val roles = data.getAllRoles()

        data.features.forEach { feature ->
            ui.segments {
                ui.segment {
                    ui.header H2 { +feature.name }
                }

                feature.groups.forEach { group ->
                    ui.segment {
                        ui.header H3 { +group.name }
                    }

                    ui.fitted.segment {
                        ui.bottom.attached.celled.striped.small.table Table {
                            thead {
                                tr {
                                    css {
                                        position = Position.sticky
                                        top = 60.px
                                    }
                                    th {
                                        +"Endpoints \\ Roles"
                                    }
                                    roles.forEach { role ->
                                        th(classes = "center aligned") {
                                            attributes["title"] = role
                                            role.split(":").forEach {
                                                span {
                                                    css {
                                                        display = Display.block
                                                        whiteSpace = WhiteSpace.nowrap
                                                    }
                                                    +it
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            tbody {
                                group.endpoints.forEach { endpoint ->
                                    tr(
                                        classes = when {
                                            endpoint.hasProblems -> "red"
                                            endpoint.allowsSensitiveData -> "yellow"
                                            else -> null
                                        }
                                    ) {
                                        td {
                                            ui.list {
                                                noui.item {
                                                    noui.header {
                                                        +endpoint.name

                                                        if (endpoint.allowsSensitiveData) {
                                                            +" "
                                                            icon.yellow.user_secret()
                                                        }
                                                    }
                                                    noui.description { +endpoint.uri }
                                                }

                                                if (endpoint.description.isNotBlank()) {
                                                    noui.item {
                                                        +endpoint.description
                                                    }
                                                }

                                                noui.item {
                                                    details {
                                                        summary { +"Response Type" }
                                                        +endpoint.responseType
                                                    }
                                                }

                                                if (endpoint.sensitiveDataExposure.isNotEmpty()) {
                                                    noui.item {
                                                        details {
                                                            summary { +"Sensitive Data Exposure" }
                                                            ui.list {
                                                                endpoint.sensitiveDataExposure.forEach { info ->
                                                                    noui.item {
                                                                        if (info.isError) {
                                                                            icon.red.exclamation_triangle()
                                                                        } else {
                                                                            icon.blue.info_circle()
                                                                        }
                                                                        +info.path
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        roles.forEach { role ->
                                            td(classes = "center aligned collapsing") {
                                                when (endpoint.getAccessLevelOfRole(role)) {
                                                    ApiAccessLevel.Granted -> {
                                                        icon.green.check()
                                                    }

                                                    ApiAccessLevel.Partial -> {
                                                        icon.yellow.user_check()
                                                    }

                                                    ApiAccessLevel.Denied -> {
                                                        // noop
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
            }
        }
    }
}
