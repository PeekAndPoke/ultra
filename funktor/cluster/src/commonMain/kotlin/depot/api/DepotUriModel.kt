package de.peekandpoke.funktor.cluster.depot.api

import kotlinx.serialization.Serializable

@Serializable
data class DepotUriModel(
    val repo: String,
    val path: String,
    val uri: String,
) {
    companion object {
        const val protocol = "depot"
    }
}
