package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.funktor.inspect.introspection.api.AuthRealmInfo
import io.peekandpoke.funktor.inspect.renderDefault
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

class AuthRealmsPage(ctx: Ctx<Props>) : Component<AuthRealmsPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: IntrospectionUi,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val loader = dataLoader {
        props.ui.api
            .getAuthRealms()
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Auth Realms") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Auth Realms" }
            }

            loader.renderDefault(this) { data ->
                renderRealms(data)
            }
        }
    }

    private fun FlowContent.renderRealms(realms: List<AuthRealmInfo>) {
        realms.forEach { realm ->
            ui.segment {
                ui.header H3 { +"Realm: ${realm.id}" }

                ui.list {
                    ui.item {
                        ui.text B { +"Password Policy: " }
                        +realm.passwordPolicyDescription
                    }
                    ui.item {
                        ui.text B { +"Regex: " }
                        +realm.passwordPolicyRegex
                    }
                }

                if (realm.providers.isNotEmpty()) {
                    ui.sub.header H4 { +"Providers" }

                    ui.striped.selectable.table Table {
                        thead {
                            tr {
                                th { +"ID" }
                                th { +"Type" }
                                th { +"Capabilities" }
                            }
                        }
                        tbody {
                            realm.providers.forEach { provider ->
                                tr {
                                    td { +provider.id }
                                    td { +provider.type }
                                    td { +provider.capabilities.joinToString(", ") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
