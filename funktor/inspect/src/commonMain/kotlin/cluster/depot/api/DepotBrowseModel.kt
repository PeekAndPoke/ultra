package io.peekandpoke.funktor.inspect.cluster.depot.api

import depot.api.DepotRepositoryModel
import kotlinx.serialization.Serializable

@Serializable
data class DepotBrowseModel(
    val repo: DepotRepositoryModel,
    val item: DepotItemModel,
    /** The file's metadata: what its writer gave, plus the depot's own entries such as `depot-subject`. */
    val meta: Map<String, String>?,
    val children: List<DepotItemModel>,
) {
    val path get() = item.path
    val parentPath get() = item.parentPath
    val isRoot get() = parentPath == "/" || parentPath.isEmpty()
}
