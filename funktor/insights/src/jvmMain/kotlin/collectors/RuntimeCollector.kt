package de.peekandpoke.funktor.insights.collectors

import com.sun.management.UnixOperatingSystemMXBean
import de.peekandpoke.funktor.insights.InsightsCollector
import de.peekandpoke.funktor.insights.InsightsCollectorData
import de.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import io.ktor.server.application.*
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.title
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean

class RuntimeCollector : InsightsCollector {

    data class Data(
        val jvmVersion: String,
        val maxMem: Long,
        val reservedMem: Long,
        val freeMem: Long,
        val cpus: Int,
        val kotlinVersion: String,
        val openFileDescriptors: Long,
        val maxFileDescriptors: Long,
        val systemProperties: Map<String, String>,
    ) : InsightsCollectorData {

        override fun renderBar(template: InsightsBarTemplate) = with(template) {

            val maxGb = "%.2f".format(maxMem / 1_000_000_000.0)
            val reservedGb = "%.2f".format(reservedMem / 1_000_000_000.0)
            val freeGb = "%.2f".format(freeMem / 1_000_000_000.0)

            val memory = "$freeGb / $reservedGb / $maxGb GB"

            left {

                ui.item {
                    title = "Number of available processors"
                    icon.microchip()
                    +"$cpus CPUs"
                }

                ui.item {
                    title = "Memory usage: free / reserved / max"
                    +memory
                }
            }
        }

        override fun renderDetails(template: InsightsGuiTemplate) = with(template) {

            menu {
                icon.microchip()
                +"Runtime"
            }

            content {

                stats()

                ui.segment {
                    ui.header { +"System properties" }

                    ui.list {
                        systemProperties.forEach { (k, v) ->
                            ui.item {
                                +"$k: $v"
                            }
                        }
                    }
                }
            }
        }

        fun FlowContent.stats() {
            ui.horizontal.segments {

                ui.center.aligned.segment {
                    ui.header { +"JVM" }
                    +jvmVersion
                }

                ui.center.aligned.segment {
                    ui.header { +"Kotlin" }
                    +kotlinVersion
                }

                ui.center.aligned.segment {
                    ui.header { +"CPUs" }
                    +cpus.toString()
                }

                ui.center.aligned.segment {
                    ui.header { +"Free Heap" }
                    +"%d MB".format(freeMem / (1024 * 1024))
                }

                ui.center.aligned.segment {
                    ui.header { +"Reserved Heap" }
                    +"%d MB".format(reservedMem / (1024 * 1024))
                }

                ui.center.aligned.segment {
                    ui.header { +"Max Heap" }
                    +"%d MB".format(maxMem / (1024 * 1024))
                }

                ui.center.aligned.segment {
                    ui.header { +"File descriptors" }
                    div { +"open: $openFileDescriptors" }
                    div { +"max: $maxFileDescriptors" }
                }
            }
        }
    }

    override fun finish(call: ApplicationCall): InsightsCollectorData {

        val rt = Runtime.getRuntime()

        val os: OperatingSystemMXBean? = ManagementFactory.getOperatingSystemMXBean()

        val openFileDesc = if (os is UnixOperatingSystemMXBean) {
            os.openFileDescriptorCount
        } else {
            0
        }
        val maxFileDesc = if (os is UnixOperatingSystemMXBean) {
            os.maxFileDescriptorCount
        } else {
            0
        }

        return Data(
            jvmVersion = System.getProperty("java.version"),
            maxMem = rt.maxMemory(),
            reservedMem = rt.totalMemory(),
            freeMem = rt.freeMemory(),
            cpus = rt.availableProcessors(),
            kotlinVersion = KotlinVersion.CURRENT.toString(),
            openFileDescriptors = openFileDesc,
            maxFileDescriptors = maxFileDesc,
            systemProperties = System.getProperties().map { (k, v) -> k.toString() to v.toString() }.toMap()
        )
    }
}
