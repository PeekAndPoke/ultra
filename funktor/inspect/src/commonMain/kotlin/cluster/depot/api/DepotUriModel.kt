package io.peekandpoke.funktor.inspect.cluster.depot.api

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
