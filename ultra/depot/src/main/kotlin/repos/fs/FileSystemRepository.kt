package io.peekandpoke.ultra.depot.repos.fs

import io.peekandpoke.ultra.common.ensureDirectory
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.depot.DepotException
import io.peekandpoke.ultra.depot.domain.DepotFileContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.DEPOT_META_DIR
import io.peekandpoke.ultra.depot.repos.DepotInvalidPathException
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.depot.repos.validateDepotPath
import java.io.File
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.StandardCopyOption
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * A depot repository in a folder of the file system. A file's metadata lives beside the files, in a tree of its own:
 * `<root>/.meta/<path>.json`.
 */
abstract class FileSystemRepository<in T>(override val name: String, dir: String) : DepotRepository<T> {

    data class FsFileContent(private val file: File) : DepotFileContent {
        override suspend fun getContentBytes(): ByteArray {
            return file.readBytes()
        }
    }

    /** A file; [path] is the path it was asked for, while [file] is where it lies, symlinks resolved. */
    data class FsFile(
        override val repo: FileSystemRepository<*>,
        private val file: File,
        override val path: String,
    ) : DepotItem.File {

        // The name as asked for too: a link keeps its own name, not its target's
        override val name: String = path.substringAfterLast('/').ifEmpty { file.name }

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

    /** A folder; [path] is the path it was asked for, while [file] is where it lies, symlinks resolved. */
    data class FsFolder(
        override val repo: FileSystemRepository<*>,
        private val file: File,
        override val path: String,
    ) : DepotItem.Folder {

        // The name as asked for too: a link keeps its own name, not its target's
        override val name: String = path.substringAfterLast('/').ifEmpty { file.name }

        override val lastModifiedAt: MpInstant? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                MpInstant.fromEpochMillis(file.lastModified())
            } catch (_: SecurityException) {
                null
            }
        }
    }

    private val json = Json

    internal val root = File(dir).absoluteFile

    /**
     * The root with symlinks resolved: every file this repository touches lies below it. Read on every call, not
     * cached: a root that is a link may be re-pointed while the app runs.
     */
    private val canonicalRoot: File get() = root.canonicalFile

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

        return try {
            if (Files.isSymbolicLink(file.toPath()) && !file.exists()) throw DepotInvalidPathException()

            val canonical = file.canonicalFile

            if (!canonical.toPath().startsWith(canonicalRoot.toPath())) throw DepotInvalidPathException()

            canonical
        } catch (_: InvalidPathException) {
            // A name the platform cannot encode (an unpaired surrogate, a non-ASCII name under a C locale)
            throw DepotInvalidPathException()
        }
    }

    /** [path] as this repository reports it: `/`-separated (also for a Windows `\`), without empty segments. */
    private fun normalised(path: String): String =
        path.split('/', File.separatorChar).filter { it.isNotEmpty() }.joinToString("/")

    /** The path of [name] inside the folder at [parent] (a normalised path). */
    private fun childPath(parent: String, name: String): String = if (parent.isEmpty()) name else "$parent/$name"

    override val type = "File system"

    override val location: String = root.absolutePath

    /**
     * The path of [file] in this repository, `/`-separated on every platform: it goes back into [resolve]. For a file
     * under the canonical root.
     */
    fun getPathInRepo(file: File): String {
        return file.relativeTo(canonicalRoot).invariantSeparatorsPath
    }

    override suspend fun listItems(path: String): List<DepotItem> {
        val dir = resolve(path)
        // The entries carry the folder's path as asked for, also when the folder is a link inside the root
        val parent = normalised(path)

        return (dir.listFiles() ?: emptyArray())
            // The metadata tree is not part of the repository's content
            .filter { parent.isNotEmpty() || it.name != DEPOT_META_DIR }
            // A symlink pointing out of the root is not listed; any other entry of a resolved folder is inside it
            .filter { !Files.isSymbolicLink(it.toPath()) || isInside(it) }
            .mapNotNull {
                when {
                    it.isFile -> FsFile(repo = this, file = it, path = childPath(parent, it.name))
                    it.isDirectory -> FsFolder(repo = this, file = it, path = childPath(parent, it.name))
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
            file.exists() && file.isFile -> FsFile(repo = this, file = file, path = normalised(path))
            else -> null
        }
    }

    override suspend fun getFolder(path: String): FsFolder? {
        val file = resolve(path)

        return when {
            file.exists() && file.isDirectory -> FsFolder(repo = this, file = file, path = normalised(path))
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

    override suspend fun getMeta(path: String): Map<String, String>? {
        val file = resolve(path)

        if (!file.isFile) return null

        val meta = metaFile(path)

        // A file stored without metadata, or before there was any
        if (!meta.isFile) return emptyMap()

        return try {
            json.decodeFromString(JsonObject.serializer(), meta.readText())
                .mapValues { (_, value) -> value.jsonPrimitive.content }
        } catch (_: SerializationException) {
            throw DepotException("The metadata of '$name/${normalised(path)}' cannot be read")
        } catch (_: IllegalArgumentException) {
            throw DepotException("The metadata of '$name/${normalised(path)}' cannot be read")
        }
    }

    /**
     * Where the metadata of the file at [path] lives: `<root>/.meta/<path>.json`. For a [path] that passed [resolve];
     * the same containment check applies.
     */
    private fun metaFile(path: String): File {
        val file = File(File(root, DEPOT_META_DIR), normalised(path) + ".json").absoluteFile

        if (!isInside(file)) throw DepotInvalidPathException()

        return file
    }

    /** Writes [stored] for [path]: to a temporary file first, then moved in place, so a reader never sees half of it. */
    private fun writeMeta(path: String, stored: Map<String, String>) {
        val target = metaFile(path)
        target.parentFile.ensureDirectory()

        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(stored.mapValues { JsonPrimitive(it.value) })))

        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    override suspend fun putFile(
        subject: DepotSubjectId<T>,
        path: String,
        content: ByteArray,
        meta: Map<String, String>,
    ): DepotItem {
        val file = resolve(path)

        val stored = DepotMetadata.validateSize(DepotMetadata.validate(meta) + (DepotMetadata.SUBJECT to subject.value))

        // The metadata first: a file is never there without it. One left behind by a failed write reads as
        // "no such file" and is replaced by the next write.
        writeMeta(path, stored)

        file.parentFile.ensureDirectory()
        file.writeBytes(content)

        return FsFile(repo = this, file = file, path = normalised(path))
    }

    /** Remove a file with a certain uri */
    override suspend fun removeFile(path: String): Boolean {
        val file = resolve(path)

        // A link to a file, lying in a folder inside the root, is removed itself: deleting through it would delete the
        // file it points to. Never the root (path ""), never a link to a folder, never a link outside the root.
        val link = File(root, path).absoluteFile
        val linkInside = link != root && link.parentFile?.let { isInside(it) } == true

        val removed = when {
            Files.isSymbolicLink(link.toPath()) -> linkInside && link.isFile && link.delete()
            else -> file.exists() && file.isFile && file.delete()
        }

        // Its metadata goes with it
        if (removed) metaFile(path).delete()

        return removed
    }
}
