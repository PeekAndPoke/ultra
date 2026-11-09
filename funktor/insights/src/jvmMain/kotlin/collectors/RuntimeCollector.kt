package io.peekandpoke.funktor.insights.collectors

import com.sun.management.UnixOperatingSystemMXBean
import io.ktor.server.application.ApplicationCall
import io.peekandpoke.funktor.insights.HeaderLogging
import io.peekandpoke.funktor.insights.InsightsCollector
import io.peekandpoke.funktor.insights.InsightsCollectorData
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean

class RuntimeCollector : InsightsCollector {

    override val key = KEY

    companion object {
        const val KEY = "runtime"

        /**
         * System property names whose value is replaced by [HeaderLogging.REDACTED] (ULTRA-00225): the header rule's
         * pattern ([HeaderLogging.sensitiveByName]), plus `key`, `pass` and `pwd`, which property names use more
         * than headers do (`javax.net.ssl.keyStorePassword`, `aws.accessKeyId`, `db.pwd`).
         */
        private val sensitivePropertyName = Regex(".*(key|pass|pwd).*")

        /** `sun.java.command` is the main class AND its arguments, where a `--password=` would sit. */
        private val alwaysRedacted = setOf("sun.java.command")

        /**
         * Values that carry a credential whatever the name: a URL with a password in its userinfo
         * (`mongodb://root:pw@db`) and a `password=` / `pwd=` parameter (a JDBC URL). Config overrides arrive as `-D`
         * properties, so a connection string lands here under a harmless name.
         */
        private val sensitivePropertyValue = Regex("""(?i).*(://[^/@\s]+:[^/@\s]*@|(password|passwd|pwd)=).*""")

        /** [properties] with every value whose name or content looks sensitive replaced by [HeaderLogging.REDACTED]. */
        internal fun redact(properties: Map<String, String>): Map<String, String> =
            properties.mapValues { (name, value) ->
                val lower = name.lowercase()
                val sensitive = lower in alwaysRedacted ||
                        HeaderLogging.sensitiveByName.matches(lower) ||
                        sensitivePropertyName.matches(lower) ||
                        sensitivePropertyValue.matches(value)

                if (sensitive) HeaderLogging.REDACTED else value
            }
    }

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
    ) : InsightsCollectorData


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
            systemProperties = redact(System.getProperties().map { (k, v) -> k.toString() to v.toString() }.toMap()),
        )
    }
}
