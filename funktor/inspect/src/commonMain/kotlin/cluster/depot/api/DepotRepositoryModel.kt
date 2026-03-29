package depot.api

import kotlinx.serialization.Serializable

@Serializable
data class DepotRepositoryModel(
    /** Unique name of the repository */
    val name: String,
    /** Human-readable type of the repository */
    val type: String,
    /** Human-readable storage location of the repository */
    val location: String,
)
