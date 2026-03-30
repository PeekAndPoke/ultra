package io.peekandpoke.funktor.cluster.depot.repos.fs

import io.peekandpoke.funktor.cluster.depot.domain.DepotFileContent
import io.peekandpoke.funktor.cluster.depot.domain.DepotItem
import io.peekandpoke.funktor.cluster.depot.repos.DepotPutFileOptions
import io.peekandpoke.funktor.cluster.depot.repos.DepotRepository
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotItemModel
import io.peekandpoke.ultra.common.ensureDirectory
import io.peekandpoke.ultra.datetime.MpInstant
import java.io.File

abstract class FileSystemRepository(override val name: String, dir: String) : DepotRepository {

    companion object {
        private fun String.isInvalidName() = contains("..")

        internal fun String.validateName() = this.apply {
            if (isInvalidName()) {
                error("Invalid name")
            }
        }
    }

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

    override val type = "File system"

    override val location: String = root.absolutePath

    fun getPathInRepo(file: File): String {
        return file.toRelativeString(root)
    }

    override suspend fun listItems(path: String): List<DepotItem> {
        // SECURITY. Prevent path traversal! check for any slashes and dot-dots
        path.validateName()

        val dir = File(root, path).absoluteFile

        return (dir.listFiles() ?: emptyArray())
            .mapNotNull {
                when {
                    it.isFile -> FsFile(repo = this, file = it)
                    it.isDirectory -> FsFolder(repo = this, file = it)
                    else -> null
                }
            }
            .onEach { it.path.validateName() }
    }

    override suspend fun listItems(path: String, limit: Int): List<DepotItem> {
        return listItems().take(limit)
    }

    override suspend fun getItem(path: String): DepotItem? {
        return getFile(path) ?: getFolder(path)
    }

    override suspend fun getFile(path: String): FsFile? {
        // SECURITY. Prevent path traversal! check for any slashes and dot-dots
        path.validateName()

        val file = File(root, path)

        return when {
            file.exists() && file.isFile -> FsFile(repo = this, file = File(root, path))
            else -> null
        }
    }

    override suspend fun getFolder(path: String): FsFolder? {
        // SECURITY. Prevent path traversal! check for any slashes and dot-dots
        path.validateName()

        val file = File(root, path)

        return when {
            file.exists() && file.isDirectory -> FsFolder(repo = this, file = File(root, path))
            else -> null
        }
    }

    override suspend fun getContent(path: String): FsFileContent? {
        // SECURITY. Prevent path traversal! check for any slashes and dot-dots
        path.validateName()

        val file = File(root, path)

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

        // SECURITY. Prevent path traversal! check for any slashes and dot-dots
        path.validateName()

        val file = File(root, path).absoluteFile

        file.parentFile.ensureDirectory()

        // TODO: apply encryption to [content]
        file.writeBytes(content)

        return FsFile(repo = this, file = file)
    }

    /** Remove a file with a certain uri */
    override suspend fun removeFile(path: String): Boolean {
        // SECURITY. Prevent path traversal! check for any slashes and dot-dots
        path.validateName()

        val file = File(root, path)

        return file.exists() && file.isFile && file.delete()
    }
}
