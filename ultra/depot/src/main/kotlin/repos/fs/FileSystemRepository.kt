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
import io.peekandpoke.ultra.depot.repos.isDepotMetaDir
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
 * A depot repository in a folder of the file system. A file's metadata lives beside the files, in a tree of its own
 * that mirrors them: `<root>/.meta/files/<path>`.
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

    /** The metadata tree's folder, symlinks resolved: `<root>/.meta`. */
    private val metaRoot: File get() = File(canonicalRoot, DEPOT_META_DIR).canonicalFile

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
            // The metadata tree, however it was reached: another spelling on a case-insensitive file system, a
            // Windows short name, a link that aliases the root
            if (canonical.toPath().startsWith(metaRoot.toPath())) throw DepotInvalidPathException()

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
            // The metadata tree is not part of the repository's content, under any spelling or alias of the root
            .filter { !it.name.isDepotMetaDir() || !it.canonicalFile.toPath().startsWith(metaRoot.toPath()) }
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

        val meta = metaFile(file)

        // No subject recorded: a file from before there was metadata, or one put there by something else
        if (!meta.isFile) return emptyMap()

        return try {
            json.decodeFromString(JsonObject.serializer(), meta.readText())
                .mapValues { (_, value) -> value.jsonPrimitive.content }
        } catch (_: SerializationException) {
            throw DepotException("The metadata of a file in '$name' cannot be read")
        } catch (_: IllegalArgumentException) {
            throw DepotException("The metadata of a file in '$name' cannot be read")
        }
    }

    /**
     * Where the metadata of [resolved] lives: `<root>/.meta/files/<its path>`, a mirror of the files. Keyed by the
     * file itself, not by the path it was asked for, so a link and its target share one. A path is never a file and
     * a folder at once, so neither are two entries of the mirror.
     */
    private fun metaFile(resolved: File): File {
        val file = File(File(metaRoot, META_FILES), getPathInRepo(resolved)).absoluteFile

        // Inside the mirror, and inside the root: a `.meta` that is a link out of the root is refused
        if (!file.canonicalFile.toPath().startsWith(metaRoot.toPath()) || !isInside(file)) throw DepotInvalidPathException()

        return file
    }

    /** A new temporary file in `<root>/.meta/tmp`, on the files' own file system so a move from it is atomic. */
    private fun tempFile(): File {
        // Reached only after [metaFile] has checked that the tree lies inside the root
        val dir = File(metaRoot, META_TMP).ensureDirectory()

        return Files.createTempFile(dir.toPath(), "put-", ".tmp").toFile()
    }

    /**
     * Moves [temp] to [target] in one step. In the mirror ([orphans]), a folder left where a file goes is removed;
     * a content move onto a folder fails.
     */
    private fun moveInPlace(temp: File, target: File, orphans: Boolean = false) {
        if (orphans && target.isDirectory && !Files.isSymbolicLink(target.toPath())) target.deleteRecursively()

        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** Creates the folders above [meta] in the mirror; a file left where a folder goes (an orphan) is removed. */
    private fun ensureMetaParent(meta: File) {
        var dir: File? = meta.parentFile

        while (dir != null && dir != metaRoot) {
            if (dir.isFile) dir.delete()
            dir = dir.parentFile
        }

        meta.parentFile.ensureDirectory()
    }

    /** Removes the metadata of [resolved], and the folders of the mirror it leaves empty. */
    private fun removeMeta(resolved: File) {
        val meta = metaFile(resolved)
        meta.delete()

        val top = File(metaRoot, META_FILES)
        var dir: File? = meta.parentFile

        // Folders only: below a file there is no folder, and its metadata file stays
        while (dir != null && dir != top && dir.isDirectory && !Files.isSymbolicLink(dir.toPath()) && dir.delete()) {
            dir = dir.parentFile
        }
    }

    /**
     * Writes content and metadata through temporary files, each moved in place in one step: the metadata, then the
     * content. A write that fails before the moves changes nothing, and writers of one file in this process take turns, so a file
     * never ends up with another writer's metadata. A reader between the two moves sees the new metadata with the
     * old content for that moment.
     */
    override suspend fun putFile(
        subject: DepotSubjectId<T>,
        path: String,
        content: ByteArray,
        meta: Map<String, String>,
    ): DepotItem {
        val file = resolve(path)

        val stored = DepotMetadata.stored(subject, meta)

        // A folder, the root included, is no place for a file
        if (file.isDirectory) throw DepotInvalidPathException()

        val metaTarget = metaFile(file)

        synchronized(lockFor(file)) {
            val contentTemp = tempFile()
            val metaTemp = tempFile()

            try {
                contentTemp.writeBytes(content)

                file.parentFile.ensureDirectory()

                metaTemp.writeText(
                    json.encodeToString(JsonObject.serializer(), JsonObject(stored.mapValues { JsonPrimitive(it.value) }))
                )
                ensureMetaParent(metaTarget)

                moveInPlace(metaTemp, metaTarget, orphans = true)
                moveInPlace(contentTemp, file)
            } finally {
                contentTemp.delete()
                metaTemp.delete()
            }
        }

        return FsFile(repo = this, file = file, path = normalised(path))
    }

    /** Remove a file with a certain uri */
    override suspend fun removeFile(path: String): Boolean {
        val file = resolve(path)

        // A link to a file, lying in a folder inside the root, is removed itself: deleting through it would delete the
        // file it points to. Never the root (path ""), never a link to a folder, never a link outside the root.
        val link = File(root, path).absoluteFile
        val linkInside = link != root && link.parentFile?.let { isInside(it) } == true

        if (Files.isSymbolicLink(link.toPath())) {
            // The metadata belongs to the file the link points to, and stays with it
            return linkInside && link.isFile && link.delete()
        }

        return synchronized(lockFor(file)) {
            val removed = file.isFile && file.delete()

            // Its metadata goes with it; so does metadata left behind without a file
            if (!file.exists()) removeMeta(file)

            removed
        }
    }

    /**
     * One of a fixed set of locks, by the file: writers and removers of one file take turns, in every repository
     * object of this process (a repository may be created per request).
     */
    private fun lockFor(file: File): Any = locks[Math.floorMod(file.path.hashCode(), locks.size)]

    private companion object {
        val locks = Array(64) { Any() }

        /** In `<root>/.meta`: the mirror of the files, holding each one's metadata. */
        const val META_FILES = "files"

        /** In `<root>/.meta`: temporary files of writes under way. */
        const val META_TMP = "tmp"
    }
}
