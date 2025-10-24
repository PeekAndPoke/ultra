package de.peekandpoke.funktor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppVersion(
    val project: String = N_A,
    val version: String = N_A,
    val gitBranch: String = N_A,
    val gitRev: String = N_A,
    val gitDesc: String = N_A,
    val date: String = N_A,
) {
    companion object {
        private const val N_A = "n/a"
    }

    val isAvailable get() = gitBranch != N_A

    fun describeGit() = listOfNotNull(
        gitBranch.takeIf { it != N_A },
        gitRev.takeIf { it != N_A },
    ).joinToString("-")
}
