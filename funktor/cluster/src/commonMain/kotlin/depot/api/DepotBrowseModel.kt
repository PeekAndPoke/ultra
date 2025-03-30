package de.peekandpoke.ktorfx.cluster.depot.api

import depot.api.DepotRepositoryModel
import kotlinx.serialization.Serializable

@Serializable
data class DepotBrowseModel(
    val repo: DepotRepositoryModel,
    val item: DepotItemModel,
    val meta: DepotItemModel.Meta?,
    val children: List<DepotItemModel>,
) {
    val path get() = item.path
    val parentPath get() = item.parentPath
    val isRoot get() = parentPath == "/" || parentPath.isEmpty()
}
