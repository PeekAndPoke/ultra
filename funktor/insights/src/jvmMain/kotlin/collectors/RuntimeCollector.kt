package io.peekandpoke.funktor.insights.collectors

import com.sun.management.UnixOperatingSystemMXBean
import io.ktor.server.application.*
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean

class RuntimeCollector : InsightsCollector {

    /** VUE-REF: `reference/collectors/RuntimeCollector.kt` */
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
        override val key = KEY

        companion object {
            const val KEY = "runtime"
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
