package io.peekandpoke.funktor.cluster.depot.api

import depot.api.DepotRepositoryModel
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotItemModel
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotUriModel
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotMeta
import io.peekandpoke.ultra.depot.domain.DepotUri
import io.peekandpoke.ultra.depot.repos.DepotRepository

/** Maps a depot item to its api model */
fun DepotItem.asApiModel(): DepotItemModel = when (this) {
    is DepotItem.File -> DepotItemModel.File(
        path = path,
        parentPath = parentPath,
        name = name,
        depotUri = uri.asApiModel(),
        size = size,
        lastModifiedAt = lastModifiedAt,
    )

    is DepotItem.Folder -> DepotItemModel.Folder(
        path = path,
        parentPath = parentPath,
        name = name,
        depotUri = uri.asApiModel(),
        lastModifiedAt = lastModifiedAt,
    )
}

/** Maps a depot repository to its api model */
fun DepotRepository.asApiModel() = DepotRepositoryModel(
    name = name,
    type = type,
    location = location,
)

/** Maps a depot uri to its api model */
fun DepotUri.asApiModel() = DepotUriModel(repo = repo, path = path, uri = uri)

/** Maps the depot's meta information to its api model */
fun DepotMeta.asApiModel() = DepotItemModel.Meta(
    visibility = when (visibility) {
        DepotMeta.Visibility.Public -> DepotItemModel.Visibility.Public
        DepotMeta.Visibility.Private -> DepotItemModel.Visibility.Private
    },
    mimeType = mimeType,
    organisationId = organisationId,
    organisationName = organisationName,
    ownerId = ownerId,
)
