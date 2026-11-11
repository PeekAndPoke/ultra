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
import java.nio.file.Files

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
    private val canonicalRoot: File by lazy { root.canonicalFile }

    /** Whether [file] lies inside the root, also after following symlinks. */
    private fun isInside(file: File): Boolean = file.canonicalFile.toPath().startsWith(canonicalRoot.toPath())

    /**
     * The file [path] names, with symlinks resolved: a path that could leave the root throws
     * [DepotInvalidPathException] (ULTRA-00304). A dangling symlink is refused too: its canonical path is the link's
     * own, so the containment check cannot see where a write through it would land. A local writer swapping a path
     * component for a symlink between this check and the use is out of scope.
     */
    private fun resolve(path: String): File {
        val file = File(root, path.validateDepotPath()).absoluteFile

        if (Files.isSymbolicLink(file.toPath()) && !file.exists()) throw DepotInvalidPathException()

        val canonical = file.canonicalFile

        if (!canonical.toPath().startsWith(canonicalRoot.toPath())) throw DepotInvalidPathException()

        return canonical
    }

    override val type = "File system"

    override val location: String = root.absolutePath

    /**
     * The path of [file] in this repository, `/`-separated on every platform: it goes back into [resolve]. Every file
     * here comes from [resolve] or a listing of a resolved folder, so it lies under the canonical root.
     */
    fun getPathInRepo(file: File): String {
        return file.relativeTo(canonicalRoot).invariantSeparatorsPath
    }

    override suspend fun listItems(path: String): List<DepotItem> {
        val dir = resolve(path)

        return (dir.listFiles() ?: emptyArray())
            // A symlink pointing out of the root is not listed; any other entry of a resolved folder is inside it
            .filter { !Files.isSymbolicLink(it.toPath()) || isInside(it) }
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

        // A symlink inside the root is removed itself: deleting through it would delete the file it points to
        val link = File(root, path).absoluteFile
        if (Files.isSymbolicLink(link.toPath())) return link.delete()

        return file.exists() && file.isFile && file.delete()
    }
}
