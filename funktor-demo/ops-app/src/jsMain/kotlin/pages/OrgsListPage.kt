package io.peekandpoke.funktor.demo.opsapp.pages

import io.peekandpoke.funktor.demo.opsapp.Apis
import io.peekandpoke.funktor.demo.opsapp.Nav
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.funktor.saas.model.OrgModel
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

@Suppress("FunctionName")
fun Tag.OrgsListPage() = comp {
    OrgsListPage(it)
}

class OrgsListPage(ctx: NoProps) : PureComponent(ctx) {

    private val loader = dataLoader {
        Apis.orgs.list().map { it.data ?: emptyList() }
    }

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.sitemap()
                noui.content { +"Organisations" }
            }

            ui.green.button {
                onClick { evt -> router.navToUri(evt, Nav.orgEdit("_new_")) }
                icon.plus()
                +"New Organisation"
            }
        }

        loader.renderDefault(this) { orgs ->
            renderTable(orgs)
        }
    }

    private fun FlowContent.renderTable(orgs: List<OrgModel>) {
        ui.segment {
            if (orgs.isEmpty()) {
                ui.message { +"No organisations yet. Create one to get started." }
            } else {
                ui.striped.table Table {
                    thead {
                        tr {
                            th { +"Name" }
                            th { +"Slug" }
                            th { +"Status" }
                            th { +"Branches" }
                            th { +"Actions" }
                        }
                    }
                    tbody {
                        orgs.forEach { org ->
                            tr {
                                td { +org.name }
                                td { +org.slug }
                                td { +org.status.name }
                                td { +"${org.branches.size}" }
                                td {
                                    ui.small.blue.button {
                                        onClick { evt -> router.navToUri(evt, Nav.orgEdit(org.id)) }
                                        icon.edit()
                                        +"Edit"
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
