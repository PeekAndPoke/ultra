package de.peekandpoke.ktorfx.core.model

import de.peekandpoke.ultra.common.datetime.Kronos
import kotlinx.serialization.Serializable
import kotlin.math.absoluteValue

/**
 */
@Serializable
class AppInfo(val version: AppVersion) {

    val versionName: String = when (val date = version.date) {
        null -> version.describeGit() + "-${Kronos.systemUtc.microsNow()}"
        else -> version.describeGit() + "-${date}"
    }

    val versionHash: String by lazy {
        val n = versionName
        listOf(n, "$n$n", "$n$n$n", "$n$n$n$n").joinToString("-") {
            it.hashCode().absoluteValue.toString(16)
        }
    }
}

