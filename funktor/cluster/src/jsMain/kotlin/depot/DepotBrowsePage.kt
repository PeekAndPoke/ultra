package io.peekandpoke.funktor.cluster.depot

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.funktor.cluster.depot.api.DepotBrowseModel
import io.peekandpoke.funktor.cluster.depot.api.DepotItemModel
import io.peekandpoke.funktor.cluster.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.routing.JoinedPageTitle
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.routing.urlParam
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.formatDdMmmYyyyHhMmSs
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class DepotBrowsePage(ctx: Ctx<Props>) : Component<DepotBrowsePage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
        val repo: String,
        val path: String,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val path: String by urlParam("path", props.path) {
        loader.reload()
    }

    val loader = dataLoader {
        props.ui.api.depot
            .browse(repo = props.repo, path = path)
            .map { it.data!! }
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Depot", path) }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            loader.renderDefault(this) { data ->

                div {
                    ui.header H2 { +"Depot Browser ${data.repo.name}" }
                }

                ui.message {
                    +data.item.depotUri.uri
                }

                data.meta?.let { meta ->
                    ui.relaxed.divided.horizontal.list {
                        noui.item {
                            noui.content {
                                noui.header { +"MimeType" }
                                noui.description { +(meta.mimeType ?: "-") }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Visibility" }
                                noui.description { +(meta.visibility.name) }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Organisation Id" }
                                noui.description { +(meta.organisationId ?: "-") }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Organisation Name" }
                                noui.description { +(meta.organisationName ?: "-") }
                            }
                        }
                        noui.item {
                            noui.content {
                                noui.header { +"Owner Id" }
                                noui.description { +(meta.ownerId ?: "-") }
                            }
                        }
                    }
                }

                when (val item = data.item) {
                    is DepotItemModel.File -> renderFile(item)
                    is DepotItemModel.Folder -> renderTable(data)
                }
            }
        }
    }

    private fun FlowContent.renderFile(file: DepotItemModel.File) {

        ui.divider()

        ui.header {
            +"File: ${file.name}"
        }

        ui.blue.button {
            icon.download()
            +"Download"
        }
    }

    private fun FlowContent.renderTable(content: DepotBrowseModel) {
        ui.striped.selectable.table Table {
            thead {
                tr {
                    th { +"" }
                    th { +"Name" }
                    th { +"Size" }
                    th { +"Last modified" }
                }
            }
            tbody {
                tr {
                    onClick { evt ->
                        val route = when (content.isRoot) {
                            true -> props.ui.routes.depot.listRepositories()
                            false -> props.ui.routes.depot.browse(repo = props.repo, path = content.parentPath)
                        }

                        router.navToUri(evt = evt, route = route)
                    }

                    td("collapsing") { // Icon
                        icon.folder()
                    }
                    td { // Name
                        +".."
                    }
                    td { // Size
                    }
                    td { // Last Modified
                    }
                }

                content.children.forEach { item ->
                    tr {
                        onClick { evt ->
                            router.navToUri(
                                evt = evt,
                                route = props.ui.routes.depot.browse(repo = props.repo, path = item.path),
                            )
                        }

                        td("collapsing") {
                            when (item) {
                                is DepotItemModel.Folder -> icon.folder()
                                is DepotItemModel.File -> icon.file()
                            }
                        }
                        td {
                            +item.name
                        }
                        td {
                            item.size?.let { +"$it bytes" }
                        }
                        td {
                            item.lastModifiedAt?.let { +it.atSystemDefaultZone().formatDdMmmYyyyHhMmSs() }
                        }
                    }
                }
            }
        }
    }
}
