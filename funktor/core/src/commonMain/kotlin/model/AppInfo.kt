package de.peekandpoke.funktor.core.model

import de.peekandpoke.ultra.common.datetime.Kronos
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue

/**
 */
@Serializable
data class AppInfo(
    val version: AppVersion,
    val versionName: String,
    val versionHash: String,
) {
    companion object {
        fun of(version: AppVersion): AppInfo {
            val versionName: String = when (val date = version.date) {
                null -> version.describeGit() + "-${Kronos.systemUtc.microsNow()}"
                else -> version.describeGit() + "-${date}"
            }

            val versionHash: String = run {
                val n = versionName

                listOf(n, "$n$n", "$n$n$n", "$n$n$n$n").joinToString("-") {
                    it.hashCode().absoluteValue.toString(16)
                }
            }

            return AppInfo(
                version = version,
                versionName = versionName,
                versionHash = versionHash,
            )
        }
    }
}
