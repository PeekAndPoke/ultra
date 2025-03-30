package de.peekandpoke.ktorfx.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVersion(
    val group: String = na,
    val version: String = na,
    val gitBranch: String = na,
    val gitRev: String = na,
    val gitDesc: String = na,
    val date: String? = null,
) {
    val isAvailable get() = gitBranch != na

    fun describeGit() = listOfNotNull(gitBranch.takeIf { it != na }, gitRev.takeIf { it != na }).joinToString("-")

    companion object {
        const val na = "n/a"
    }
}
