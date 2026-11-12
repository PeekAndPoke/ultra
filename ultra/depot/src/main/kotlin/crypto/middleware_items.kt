package io.peekandpoke.ultra.depot.crypto

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.repos.DepotRepository

/** A file of an inner repository as an item of the middleware [repo]: its content is read through the middleware. */
internal class MiddlewareFile(override val repo: DepotRepository<*>, private val item: DepotItem.File) : DepotItem.File {
    override val path: String get() = item.path
    override val name: String get() = item.name
    override val lastModifiedAt: MpInstant? get() = item.lastModifiedAt
    override val size: Long? get() = item.size

    override suspend fun getContent(): DepotContent = repo.getContent(path)
}

/** A folder of an inner repository as an item of the middleware [repo]. */
internal class MiddlewareFolder(override val repo: DepotRepository<*>, private val item: DepotItem.Folder) : DepotItem.Folder {
    override val path: String get() = item.path
    override val name: String get() = item.name
    override val lastModifiedAt: MpInstant? get() = item.lastModifiedAt
}

/** This item of an inner repository as an item of the middleware [repo]: the inner repository is not handed out. */
internal fun DepotItem.wrappedFor(repo: DepotRepository<*>): DepotItem = when (this) {
    is DepotItem.File -> MiddlewareFile(repo, this)
    is DepotItem.Folder -> MiddlewareFolder(repo, this)
}
