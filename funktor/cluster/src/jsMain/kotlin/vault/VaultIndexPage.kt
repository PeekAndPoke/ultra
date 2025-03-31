package de.peekandpoke.funktor.cluster.vault

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.funktor.cluster.renderDefault
import de.peekandpoke.kraft.addons.routing.JoinedPageTitle
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.camelCaseSplit
import de.peekandpoke.ultra.vault.VaultModels
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.b
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.tr

class VaultIndexPage(ctx: Ctx<Props>) : Component<VaultIndexPage.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val ui: FunktorClusterUi,
    )

    private enum class Sorting {
        Name,
        IndexSize,
        DocumentsCount,
        DocumentsSize,
        DocumentsAvgSize,
    }

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var sorting by value(Sorting.Name)

    private val loader = dataLoader {
        props.ui.api.vault.listRepositories().map { it.data!! }
    }

    private fun Int.formatWithDots(dot: String = "."): String = toLong().formatWithDots(dot)

    private fun Long.formatWithDots(dot: String = "."): String {
        return toString()
            .reversed()
            .chunked(3)
            .joinToString(dot)
            .reversed()
    }

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Funktor", "Vault") }

        ui.padded.segment {
            ui.right.floated.small.basic.icon.button {
                onClick { loader.reload() }
                icon.redo()
            }

            div {
                ui.header H2 { +"Vault Repositories" }
            }

            ui.form {
                ui.field {
                    ui.horizontal.list {
                        noui.item {
                            noui.header { +"Sorting" }
                        }
                        Sorting.entries.forEach { s ->
                            noui.item {
                                ui.given(sorting != s) { basic }.blue.label.button {
                                    onClick { sorting = s }
                                    +s.name.camelCaseSplit().joinToString(" ")
                                }
                            }
                        }
                    }
                }
            }

            loader.renderDefault(this) { data ->
                val sorter: Comparator<VaultModels.RepositoryInfo> = when (sorting) {
                    Sorting.Name -> compareBy { it.name }
                    Sorting.IndexSize -> compareByDescending { it.figures.indexesSize }
                    Sorting.DocumentsCount -> compareByDescending { it.figures.documentCount }
                    Sorting.DocumentsSize -> compareByDescending { it.figures.documentsSize }
                    Sorting.DocumentsAvgSize -> compareByDescending { it.figures.avgSize }
                }

                val sorted = data.sortedWith(sorter)

                renderTable(sorted)
            }
        }
    }

    private fun FlowContent.renderTable(data: List<VaultModels.RepositoryInfo>) {
        ui.fixed.celled.selectable.table Table {
            thead {
                tr {
                    th { +"Repo" }
                    th { +"Indexes" }
                    th { +"Cache" }
                    th { +"Documents" }
                }
            }
            tbody {
                data.forEach { repo ->
                    tr("top aligned") {
                        if (repo.hasErrors) {
                            classes += setOf("red")
                        }

                        td { // Repo
                            renderTable(
                                mapOf(
                                    "Connection" to repo.connection,
                                    "Repository" to repo.name,
                                    "writeConcern" to repo.figures.writeConcern,
                                )
                            )
                        }
                        td { // healthy indexes
                            renderTable(
                                mapOf(
                                    "Healthy" to repo.indexes.healthyIndexes.size,
                                    "Missing" to repo.indexes.missingIndexes.size,
                                    "Excess" to repo.indexes.excessIndexes.size,
                                    "Size" to repo.figures.indexesSize?.formatWithDots(),
                                )
                            )
                        }
                        td { // Cache
                            renderTable(
                                mapOf(
                                    "Enabled" to repo.figures.cacheEnabled,
                                    "In use" to repo.figures.cacheInUse,
                                    "Size" to repo.figures.cacheSize?.formatWithDots(),
                                    "Usage" to repo.figures.cacheUsage?.formatWithDots(),
                                )
                            )
                        }
                        td { // Documents
                            renderTable(
                                mapOf(
                                    "Count" to repo.figures.documentCount?.formatWithDots(),
                                    "Size" to repo.figures.documentsSize?.formatWithDots(),
                                    "Avg Size" to repo.figures.avgSize?.formatWithDots(),
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun Tag.renderTable(data: Map<String, Any?>) {
        ui.fixed.very.very.basic.striped.compact.table Table {
            tbody {
                data.forEach { (k, v) ->
                    tr {
                        td { b { +k } }
                        td { +(v?.toString() ?: "n/a") }
                    }
                }
            }
        }
    }
}
