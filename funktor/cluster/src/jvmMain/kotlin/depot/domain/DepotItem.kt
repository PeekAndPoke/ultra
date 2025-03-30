package de.peekandpoke.ktorfx.cluster.depot.domain

import de.peekandpoke.ktorfx.cluster.depot.api.DepotItemModel
import de.peekandpoke.ktorfx.cluster.depot.repos.DepotRepository
import de.peekandpoke.ultra.common.datetime.MpInstant

/**
 * Definition of a depot file
 */
sealed interface DepotItem {

    interface Folder : DepotItem

    interface File : DepotItem {
        /** The size of the file in bytes */
        val size: Long?

        /** Gets the content of the file */
        suspend fun getContent(): DepotFileContent?
    }

    /** The bucket the file belongs to */
    val repo: DepotRepository

    /** The path of the item */
    val path: String

    /** Get the path to the parent item */
    val parentPath: String get() = repo.getParentPath(path)

    /** The name of the item */
    val name: String

    /** When was the file last modified */
    val lastModifiedAt: MpInstant?

    /** Fully qualifying uri of the file */
    val uri: DepotUri get() = DepotUri.of(this)

    /** Map to api model */
    fun asApiModel() = when (this) {
        is File -> DepotItemModel.File(
            path = path,
            parentPath = parentPath,
            name = name,
            depotUri = uri.asApiModel(),
            size = size,
            lastModifiedAt = lastModifiedAt,
        )

        is Folder -> DepotItemModel.Folder(
            path = path,
            parentPath = parentPath,
            name = name,
            depotUri = uri.asApiModel(),
            lastModifiedAt = lastModifiedAt,
        )
    }
}
