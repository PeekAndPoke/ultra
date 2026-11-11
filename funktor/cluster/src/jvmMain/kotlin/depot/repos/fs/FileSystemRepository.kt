package io.peekandpoke.funktor.cluster.depot.repos.fs

import io.peekandpoke.funktor.cluster.depot.domain.DepotFileContent
import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.cluster.depot.repos.DepotInvalidPathException
import io.peekandpoke.funktor.cluster.depot.repos.DepotPutFileOptions
import io.peekandpoke.funktor.cluster.depot.repos.DepotRepository
import io.peekandpoke.funktor.cluster.depot.repos.validateDepotPath
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotItemModel
import io.peekandpoke.ultra.common.ensureDirectory
import io.peekandpoke.ultra.datetime.MpInstant
import java.io.File

abstract class FileSystemRepository(override val name: String, dir: String) : DepotRepository {

    data class FsFileContent(private val file: File) : DepotFileContent {
        override suspend fun getContentBytes(): ByteArray {
            return file.readBytes()
        }
    }

    data class FsFile(
        override val repo: FileSystemRepository,
        private val file: File,
    ) : DepotItem.File {
        override val path by lazy { repo.getPathInRepo(file) }

        override val name: String = file.name

        override val size: Long? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                file.length()
            } catch (_: SecurityException) {
                null
            }
        }

        override val lastModifiedAt: MpInstant? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                MpInstant.fromEpochMillis(file.lastModified())
            } catch (_: SecurityException) {
                null
            }
        }

        override suspend fun getContent(): DepotFileContent {
            return FsFileContent(file = file)
        }
    }

    data class FsFolder(
        override val repo: FileSystemRepository,
        private val file: File,
    ) : DepotItem.Folder {
        override val path by lazy { repo.getPathInRepo(file) }

        override val name: String = file.name

        override val lastModifiedAt: MpInstant? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                MpInstant.fromEpochMillis(file.lastModified())
            } catch (_: SecurityException) {
                null
            }
        }
    }

    internal val root = File(dir).absoluteFile

    /** The root with symlinks resolved: every file this repository touches lies below it. */
    private val canonicalRoot by lazy { root.canonicalFile.toPath() }

    /** Whether [file] lies inside the root, also after following symlinks. */
    private fun isInside(file: File): Boolean = file.canonicalFile.toPath().startsWith(canonicalRoot)

    /** The file [path] names: a path that could leave the root throws [DepotInvalidPathException] (ULTRA-00304). */
    private fun resolve(path: String): File {
        val file = File(root, path.validateDepotPath()).absoluteFile

        if (!isInside(file)) throw DepotInvalidPathException(path)

        return file
    }

    override val type = "File system"

    override val location: String = root.absolutePath

    fun getPathInRepo(file: File): String {
        return file.toRelativeString(root)
    }

    override suspend fun listItems(path: String): List<DepotItem> {
        val dir = resolve(path)

        return (dir.listFiles() ?: emptyArray())
            // A symlink pointing out of the root is not listed
            .filter { isInside(it) }
            .mapNotNull {
                when {
                    it.isFile -> FsFile(repo = this, file = it)
                    it.isDirectory -> FsFolder(repo = this, file = it)
                    else -> null
                }
            }
    }

    override suspend fun listItems(path: String, limit: Int): List<DepotItem> {
        return listItems(path).take(limit)
    }

    override suspend fun getItem(path: String): DepotItem? {
        return getFile(path) ?: getFolder(path)
    }

    override suspend fun getFile(path: String): FsFile? {
        val file = resolve(path)

        return when {
            file.exists() && file.isFile -> FsFile(repo = this, file = file)
            else -> null
        }
    }

    override suspend fun getFolder(path: String): FsFolder? {
        val file = resolve(path)

        return when {
            file.exists() && file.isDirectory -> FsFolder(repo = this, file = file)
            else -> null
        }
    }

    override suspend fun getContent(path: String): FsFileContent? {
        val file = resolve(path)

        return when {
            file.exists() && file.isFile -> FsFileContent(file)
            else -> null
        }
    }

    override suspend fun getMeta(path: String): DepotItemModel.Meta? {
        return null
    }

    override suspend fun putFile(
        path: String,
        content: ByteArray,
        options: DepotPutFileOptions.Builder.() -> Unit,
    ): DepotItem {
        // val builtOptions = depotPutFileOptions(options)

        val file = resolve(path)

        file.parentFile.ensureDirectory()

        // TODO: apply encryption to [content]
        file.writeBytes(content)

        return FsFile(repo = this, file = file)
    }

    /** Remove a file with a certain uri */
    override suspend fun removeFile(path: String): Boolean {
        val file = resolve(path)

        return file.exists() && file.isFile && file.delete()
    }
}
