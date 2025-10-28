package de.peekandpoke.funktor.insights.collectors

import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsCollectorData
import de.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.common.recursion.recurse
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBlueprint
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.domain.DebugInfo
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.server.application.*
import kotlinx.html.Unsafe
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.style
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr

class KontainerCollector(
    private val kontainer: Kontainer,
    private val blueprint: KontainerBlueprint,
) : InsightsCollector {

    data class Data(
        val numOld: Int,
        val numTotal: Int,
        val info: DebugInfo,
    ) : InsightsCollectorData {

        private val showInstanceGraphButtonId = "kontainer-show-instances-graph-button"
        private val showFullGraphButtonId = "kontainer-show-full-graph-button"
        private val graphId = "kontainer-graph"

        override fun renderBar(template: InsightsBarTemplate) = with(template) {

            left {

                ui.item {
                    title = "Kontainers in memory: young / old / total"

                    // TODO: make the magic numbers configurable
                    when {
                        numOld < 10 -> icon.green.cubes()
                        numOld < 50 -> icon.yellow.cubes()
                        else -> icon.red.cubes()
                    }

                    +"${numTotal - numOld} / $numOld / $numTotal"
                }
            }
        }

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.cubes()
                +"Kontainer"
            }

            content {

                ui.header H2 { +"Services" }

                ui.green.button {
                    id = showInstanceGraphButtonId
                    +"Show instances Graph"
                }

                ui.red.button {
                    id = showFullGraphButtonId
                    +"Show Full Graph"
                }

                div {
                    id = graphId
                    style = "display: none; width: 100%; height: 85vh; border: 1px solid #888;"
                }

                ui.sortable.celled.table Table {
                    thead {
                        tr {
                            th { +"Service" }
                            th { +"Type" }
                            th { +"Instances" }
                            th { +"Injects" }
                            th { +"Definition & Overwrites" }
                        }
                    }
                    tbody {
                        info.services
                            .sortedWith(
                                compareBy(
                                    { if (it.instances.isEmpty()) 1 else 0 },
                                    { it.cls.fqn },
                                )
                            )
                            .forEach { service ->
                                tr {
                                    td("top aligned") { +service.cls.fqn }
                                    td("top aligned") { +service.type.toString() }
                                    td("top aligned") {
                                        ui.divided.list {
                                            service.instances.forEach { instance ->
                                                noui.item {
                                                    noui.header { +instance.cls.fqn }
                                                    noui.meta { +"${instance.createdAt}" }
                                                }
                                            }
                                        }
                                    }
                                    td("top aligned") {
                                        ui.divided.list {
                                            service.definition.injects.forEach { inject ->
                                                noui.item {
                                                    noui.header { +inject.name }
                                                    noui.list {
                                                        inject.classes.forEach { cls ->
                                                            noui.item {
                                                                +cls.fqn
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    td("top aligned") {
                                        ui.divided.list {
                                            service.definition.recurse { overwrites }.forEachIndexed { idx, def ->
                                                noui.item {
                                                    noui.header {
                                                        when (idx) {
                                                            0 -> +""
                                                            else -> +"Overwrites -> "
                                                        }
                                                        +"${def.injectionType} - ${def.creates.fqn}"
                                                    }
                                                    noui.description {
                                                        +def.codeLocation.location
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

            inlineScript {
                +"""
                    let network = null;
                """.trimIndent()

                renderInstancesGraphJs(template)
                renderFullGraphJs(template)
            }
        }

        @Suppress("DuplicatedCode")
        private fun Unsafe.renderInstancesGraphJs(template: InsightsGuiTemplate) {

            val buttonId = showInstanceGraphButtonId
            val containerId = graphId

            val classIdMap = mutableMapOf<DebugInfo.ClassInfo, Int>()

            fun getId(cls: DebugInfo.ClassInfo): Int = classIdMap.getOrPut(cls) {
                classIdMap.size + 1
            }

            val filteredServices = info.services.filter { it.instances.isNotEmpty() }

            // Create nodes
            val nodes = filteredServices.map { service ->
                mapOf(
                    "id" to getId(service.cls),
                    "value" to service.definition.injects.sumOf { it.classes.size },
                    "label" to service.cls.fqn.split(".").last() + "\n" + service.type.toString(),
                    "color" to when (service.type) {
                        ServiceProvider.Type.Singleton -> "#1E90FF"
                        ServiceProvider.Type.Prototype -> "#FFD700"
                        ServiceProvider.Type.Dynamic -> "#8B0000"
                        ServiceProvider.Type.SemiDynamic -> "#9400D3"
                        ServiceProvider.Type.DynamicOverride -> "#DC143C"
                    },
                )
            }

            // Create edges
            val edges = filteredServices.flatMap { service ->
                service.definition.injects.flatMap { inject ->
                    inject.classes.map { cls ->
                        mapOf(
                            "from" to getId(service.cls),
                            "to" to getId(cls),
                            "arrows" to "to",
                            "dashes" to when (inject.provisionType) {
                                DebugInfo.ParamInfo.ProvisionType.Direct -> false
                                DebugInfo.ParamInfo.ProvisionType.Lazy -> true
                            },
                            "value" to when (inject.provisionType) {
                                DebugInfo.ParamInfo.ProvisionType.Direct -> 2
                                DebugInfo.ParamInfo.ProvisionType.Lazy -> 1
                            },
                        )
                    }
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
                              avoidOverlap: 0.1,
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
                              max: 2,
                            },
                        },
                    };
                    
                    network = new vis.Network(container, data, options);
                }
            """.trimIndent()
        }

        @Suppress("DuplicatedCode")
        private fun Unsafe.renderFullGraphJs(template: InsightsGuiTemplate) {

            val buttonId = showFullGraphButtonId
            val containerId = graphId

            val classIdMap = mutableMapOf<DebugInfo.ClassInfo, Int>()

            fun getId(cls: DebugInfo.ClassInfo): Int = classIdMap.getOrPut(cls) {
                classIdMap.size + 1
            }

            // Create nodes
            val nodes = info.services.map { service ->
                mapOf(
                    "id" to getId(service.cls),
                    "value" to service.definition.injects.sumOf { it.classes.size },
                    "label" to service.cls.fqn.split(".").last() + "\n" + service.type.toString(),
                    "color" to when (service.type) {
                        ServiceProvider.Type.Singleton -> "#1E90FF"
                        ServiceProvider.Type.Prototype -> "#FFD700"
                        ServiceProvider.Type.Dynamic -> "#8B0000"
                        ServiceProvider.Type.SemiDynamic -> "#9400D3"
                        ServiceProvider.Type.DynamicOverride -> "#DC143C"
                    },
                )
            }

            // Create edges
            val edges = info.services.flatMap { service ->
                service.definition.injects.flatMap { inject ->
                    inject.classes.map { cls ->
                        mapOf(
                            "from" to getId(service.cls),
                            "to" to getId(cls),
                            "arrows" to "to",
                            "dashes" to when (inject.provisionType) {
                                DebugInfo.ParamInfo.ProvisionType.Direct -> false
                                DebugInfo.ParamInfo.ProvisionType.Lazy -> true
                            },
                            "value" to when (inject.provisionType) {
                                DebugInfo.ParamInfo.ProvisionType.Direct -> 2
                                DebugInfo.ParamInfo.ProvisionType.Lazy -> 1
                            },
                        )
                    }
                }
            }

            // Render raw js
            +"""
                $("#${buttonId}").on("click", () => {
                    if (!network) {
                        $("#${containerId}").show();
                        setTimeout(() => { createFullGraph(); }, 500);
                    }
                });
                                        
                function createFullGraph() {
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
                            solver: "barnesHut",
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
                              avoidOverlap: 0
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
                              max: 2,
                            },
                        },
                    };
                    
                    network = new vis.Network(container, data, options);
                }
            """.trimIndent()
        }
    }

    override fun finish(call: ApplicationCall): Data {

        return try {
            val numTotal = blueprint.tracker.getNumAlive()
            // TODO: make the threshold configurable
            val numOld = blueprint.tracker.getNumAlive(15)

            Data(
                numOld = numOld,
                numTotal = numTotal,
                info = kontainer.tools.getDebugInfo(),
            )

        } catch (e: Throwable) {
            println("[ERROR] Could not get kontainer DebugInfo!\n${e.stackTraceToString()}")

            Data(
                numOld = 0,
                numTotal = 0,
                info = DebugInfo(services = emptyList())
            )
        }
    }
}
