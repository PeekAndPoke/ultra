package de.peekandpoke.funktor.insights.collectors

import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsCollectorData
import de.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.funktor.staticweb.resources.prismjs.prism
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import de.peekandpoke.ultra.vault.domain.DatabaseGraphModel
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import de.peekandpoke.ultra.vault.tools.DatabaseGraphBuilder
import io.ktor.server.application.*
import kotlinx.html.FlowContent
import kotlinx.html.Unsafe
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.style
import kotlinx.html.title

class VaultCollector(private val profiler: QueryProfiler) : InsightsCollector {

    data class Data(
        val entries: List<QueryProfiler.Entry.Impl>,
    ) : InsightsCollectorData {

        private val showGraphButtonId = "vault-show-graph-button"
        private val graphId = "vault-graph"

        private val totalTimeNs: Long by lazy {
            entries.sumOf { it.totalNs }
        }

        private val totalSerializerNs by lazy {
            entries.sumOf { it.measureSerializer.totalNs }
        }

        private val totalQueryNs by lazy {
            entries.sumOf { it.measureQuery.totalNs }
        }

        private val totalIteratorNs by lazy {
            entries.sumOf { it.measureIterator.totalNs }
        }

        private val totalDeserializerNs by lazy {
            entries.sumOf { it.measureDeserializer.totalNs }
        }

        private val totalExplainNs by lazy {
            entries.sumOf { it.measureExplain.totalNs }
        }

        override fun renderBar(template: InsightsBarTemplate) = with(template) {

            left {

                ui.item {
                    title = "Database | Total: ${totalTimeNs.formatMs()} | " +
                            "Serializer: ${totalSerializerNs.formatMs()} | " +
                            "Query: ${totalQueryNs.formatMs()} | " +
                            "Iterator: ${totalIteratorNs.formatMs()} | " +
                            "Deserializer: ${totalDeserializerNs.formatMs()}"

                    icon.database()

                    +"${entries.size} in ${totalTimeNs.formatMs()}"
                }
            }
        }

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            val graph = template.kontainer.use(DatabaseGraphBuilder::class) { getGraph() }

            menu {
                icon.database()
                +"Database"
            }

            content {

                stats()

                graph?.let {
                    ui.red.button {
                        id = showGraphButtonId
                        +"Show Database Graph"
                    }
                    div {
                        id = graphId
                        style = "display: none; width: 100%; height: 85vh; border: 1px solid #888;"
                    }

                    ui.divider {}
                }

                entries.forEachIndexed { idx, it ->
                    ui.segment {
                        ui.header H5 {
                            +"Query #${idx + 1} took ${it.totalNs.formatMs()} - ${it.connection}"
                        }

                        ui.horizontal.segments {
                            ui.segment {
                                +"Results: ${it.count} of total ${it.totalCount ?: "n/a"}"
                            }
                            ui.segment {
                                +"Serializer: ${it.measureSerializer.totalNs.formatMs()} (${it.measureSerializer.count}x)"
                            }
                            ui.segment {
                                +"Query: ${it.measureQuery.totalNs.formatMs()} (${it.measureQuery.count}x)"
                            }
                            ui.segment {
                                +"Iterator: ${it.measureIterator.totalNs.formatMs()} (${it.measureIterator.count}x)"
                            }
                            ui.segment {
                                +"Deserializer: ${it.measureDeserializer.totalNs.formatMs()} (${it.measureDeserializer.count}x)"
                            }
                            ui.segment {
                                +"Explain: ${it.measureExplain.totalNs.formatMs()} (${it.measureExplain.count}x)"
                            }
                        }

                        prism(it.queryLanguage) { it.query }

                        json(it.vars)

                        it.queryExplained?.let { explained ->
                            prism("text") { explained }
                        }
                    }
                }

                ui.dividing.header H3 { +"Database graph" }

                ui.segment {
                    json(graph)
                }
            }

            graph?.let {
                inlineScript {
                    +"""
                    let network = null;
                """.trimIndent()

                    renderGraphJs(template, it)
                }
            }

            Unit
        }

        fun FlowContent.stats() {
            ui.horizontal.segments {
                ui.center.aligned.segment {
                    ui.header { +"Queries" }
                    +entries.size.toString()
                }

                ui.center.aligned.segment {
                    ui.header { +"Total" }
                    +totalTimeNs.formatMs()
                }

                ui.center.aligned.segment {
                    ui.header { +"Serializer" }
                    +totalSerializerNs.formatMs()
                }

                ui.center.aligned.segment {
                    ui.header { +"Query" }
                    +totalQueryNs.formatMs()
                }

                ui.center.aligned.segment {
                    ui.header { +"Iterator" }
                    +totalIteratorNs.formatMs()
                }

                ui.center.aligned.segment {
                    ui.header { +"Deserializer" }
                    +totalDeserializerNs.formatMs()
                }

                ui.center.aligned.segment {
                    ui.header { +"Explain" }
                    +totalExplainNs.formatMs()
                }
            }
        }

        @Suppress("DuplicatedCode")
        private fun Unsafe.renderGraphJs(template: InsightsGuiTemplate, graph: DatabaseGraphModel) {

            val buttonId = showGraphButtonId
            val containerId = graphId

            val idMap = mutableMapOf<DatabaseGraphModel.Repo.Id, Int>()

            fun getId(cls: DatabaseGraphModel.Repo.Id): Int = idMap.getOrPut(cls) {
                idMap.size + 1
            }

            // Create nodes
            val nodes = graph.repos.map { repo ->
                mapOf(
                    "id" to getId(repo.id),
                    "label" to repo.id.name + "\n" + repo.id.connection,
                    "value" to repo.storedClasses.sumOf { storedClass ->
                        storedClass.references.size
                    }
                )
            }

            // Create edges
            val edges = graph.repos.flatMap { repo ->
                val groups = repo.storedClasses
                    .flatMap { storedCls -> storedCls.references }
                    .filter { ref -> ref.repo != null }
                    .groupBy { ref -> ref.repo!! to ref.type }

                groups.map { (group, refs) ->

                    val (refRepo: DatabaseGraphModel.Repo.Id, refType: DatabaseGraphModel.Reference.Type) = group

                    mapOf(
                        "from" to getId(refRepo),
                        "to" to getId(repo.id),
                        "arrows" to "to",
                        "dashes" to when (refType) {
                            DatabaseGraphModel.Reference.Type.Direct -> false
                            DatabaseGraphModel.Reference.Type.Lazy -> true
                        },
                        "value" to refs.size,
                        "label" to "${refs.size}",
                    )
                }
            }

            // Render raw js
            +"""
                $("#${buttonId}").on("click", () => {
                    if (!network) {
                        $("#${containerId}").show();
                        setTimeout(() => { createInstanceGraph(); }, 500);
                    }
                });
                                        
                function createInstanceGraph() {
                    // create an array with nodes
                    let nodes = ${template.prettyPrintJson(nodes)}

                    console.log("Num nodes", nodes.length);

                    // create an array with edges
                    let edges = ${template.prettyPrintJson(edges)}

                    console.log("Num edges", edges.length);

                    // create a network
                    var container = document.getElementById("$containerId");
                    var data = {
                      nodes: nodes,
                      edges: edges,
                    };
                    var options = {
                        layout: {
                            improvedLayout: false,
                        },
                        "physics": {
                            solver: "forceAtlas2Based",
                            barnesHut: {
                              gravitationalConstant: -2000,
                              centralGravity: 0.3,
                              springLength: 95,
                              springConstant: 0.04,
                              damping: 0.09,
                              avoidOverlap: 0.0,
                            },
                            forceAtlas2Based: {
                              gravitationalConstant: -50,
                              centralGravity: 0.01,
                              springConstant: 0.08,
                              springLength: 100,
                              damping: 0.4,
                              avoidOverlap: 0.25,
                            },
                            repulsion: {
                              centralGravity: 0.2,
                              springLength: 200,
                              springConstant: 0.05,
                              nodeDistance: 100,
                              damping: 0.09
                            },
                            hierarchicalRepulsion: {
                              centralGravity: 0.0,
                              springLength: 100,
                              springConstant: 0.01,
                              nodeDistance: 120,
                              damping: 0.09
                            },
                            "maxVelocity": 50,
                            "minVelocity": 0.75,
                        },
                        nodes: {
//                            shape: "square",
                            shape: "dot",
                            scaling: {
                              min: 10,
                              max: 50,
                            },
                        },
                        edges: {
                            scaling: {
                              min: 1,
                              max: 5,
                            },
                        },
                    };
                    
                    network = new vis.Network(container, data, options);
                }
            """.trimIndent()
        }
    }

    override fun finish(call: ApplicationCall): Data {
        val entries = profiler.entries.filterIsInstance<QueryProfiler.Entry.Impl>()

        return Data(entries = entries)
    }
}
