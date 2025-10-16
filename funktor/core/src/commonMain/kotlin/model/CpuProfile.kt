package de.peekandpoke.funktor.core.model

import de.peekandpoke.ultra.common.roundWithPrecision
import kotlinx.serialization.Serializable

@Serializable
data class CpuProfile(
    val names: List<String>,
    val timeMs: Double,
    val cpuMs: Double,
    val children: List<CpuProfile> = emptyList(),
) {
    data class WithValue<T>(
        val value: T,
        val profile: CpuProfile,
    )

    val cpuUsage: Double = cpuMs / timeMs
    val cpuUsagePct: Double = cpuUsage * 100

    val totalCpuMs: Double by lazy {
        cpuMs + children.sumOf { it.totalCpuMs }
    }

    val totalCpuUsage: Double by lazy {
        cpuUsage + children.sumOf { it.totalCpuUsage }
    }

    val totalCpuUsagePct: Double by lazy { totalCpuUsage * 100 }

    fun plot(indent: String = ""): String = buildString {
        append(indent)
        append(names.joinToString(" | "))
        append(" | time = ${timeMs.roundWithPrecision(2)} ms")
        append(" | cpu time = ${cpuMs.roundWithPrecision(2)} ms")
        append(" | cpu pct = ${cpuUsagePct.roundWithPrecision(2)} %")
        appendLine()

        val newIndent = indent.replace(".".toRegex(), " ") + "+ "

        children.forEach {
            append(
                it.plot(newIndent)
            )
        }
    }
}

