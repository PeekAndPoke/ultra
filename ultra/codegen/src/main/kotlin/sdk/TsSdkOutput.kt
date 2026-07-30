package io.peekandpoke.ultra.codegen.sdk

import java.io.File

/**
 * Collects the files the SDK run produces.
 *
 * Nothing is written to disk until [writeTo], so a failed run leaves no half-generated output behind
 * and `--dry-run` / `--check` can inspect the result without touching the target directory.
 *
 * Two contributors writing the same path is a hard error naming both. With contributors arriving from
 * a DI container, a silent last-write-wins would make the output depend on registration order — the
 * exact fragility the phased contract exists to remove.
 */
class TsSdkOutput {

    /** A file the run intends to write. */
    data class Entry(
        /** Path relative to the SDK root, e.g. `models.ts`. */
        val path: String,
        val content: String,
        /** The contributor that produced it, for conflict reporting. */
        val writtenBy: String,
    )

    private val entries = LinkedHashMap<String, Entry>()

    /** All planned files, in creation order. */
    fun entries(): List<Entry> = entries.values.toList()

    /**
     * Returns a view that stamps [contributor] onto every file it writes.
     *
     * [loader] is where [Scope.resource] looks: it must be the CONTRIBUTOR's classloader, not this
     * module's, so a contributor shipping resources from its own jar still finds them under an
     * isolating loader. Defaults to this module's only for callers that own no resources.
     */
    fun scopeFor(
        contributor: String,
        loader: ClassLoader = TsSdkOutput::class.java.classLoader,
    ): Scope = Scope(contributor, loader)

    /**
     * Rejects a path that is absolute or escapes the SDK root.
     *
     * `File(baseDir, "../../etc/x")` resolves OUTSIDE `baseDir` — verified — so without this a
     * contributor could have `writeTo` overwrite an arbitrary file, and `--check` read one. Paths
     * come from contributors, which are an open extension point, so this is a boundary rather than a
     * sanity check.
     */
    private fun validatePath(path: String, contributor: String) {
        val normalized = path.replace('\\', '/')

        check(normalized.isNotBlank()) {
            "Contributor '$contributor' planned a file with a blank path."
        }

        check(!normalized.startsWith("/") && !Regex("^[A-Za-z]:").containsMatchIn(normalized)) {
            "Contributor '$contributor' planned the ABSOLUTE path '$path'. Every emitted path is " +
                    "relative to the SDK root, which the generator owns outright."
        }

        check(normalized.split('/').none { it == ".." }) {
            "Contributor '$contributor' planned the path '$path', which escapes the SDK root via " +
                    "'..'. The generator writes only inside the directory it owns."
        }
    }

    private fun add(entry: Entry) {
        validatePath(entry.path, entry.writtenBy)

        val existing = entries[entry.path]

        check(existing == null) {
            "File '${entry.path}' is written twice: by '${existing!!.writtenBy}' and by " +
                    "'${entry.writtenBy}'. Emitted paths must be unique — otherwise the SDK depends on " +
                    "contributor order. Fix: give one of them a different path, or register only one."
        }

        entries[entry.path] = entry
    }

    /** Writes every planned file under [baseDir], creating directories as needed. */
    fun writeTo(baseDir: File): List<File> = entries.values.map { entry ->
        val file = File(baseDir, entry.path)

        file.parentFile?.mkdirs()
        file.writeText(entry.content)

        file
    }

    /**
     * Compares the planned files against what is on disk under [baseDir].
     *
     * Backs the `--check` mode: a non-empty result means the checked-in SDK is stale.
     */
    fun diffAgainst(baseDir: File): List<String> = entries.values.mapNotNull { entry ->
        val file = File(baseDir, entry.path)

        when {
            !file.exists() -> "${entry.path}: missing on disk"
            file.readText() != entry.content -> "${entry.path}: differs from generated output"
            else -> null
        }
    }

    /** The API handed to a contributor during the emit phase. */
    inner class Scope internal constructor(
        private val contributor: String,
        private val loader: ClassLoader,
    ) {

        /** Plans a file at [path] with [content]. */
        fun file(path: String, content: String) {
            add(Entry(path = path, content = content, writtenBy = contributor))
        }

        /**
         * Plans a file copied from this contributor's own classpath resources.
         *
         * Hand-written runtime code (datetime helpers, the fetch transport) ships as a resource rather
         * than being generated — it is maintained by humans next to the codec it mirrors.
         */
        fun resource(resourcePath: String, to: String) {
            // `bufferedReader()` on an InputStream defaults to UTF-8 (not the platform charset), so
            // the encoding needs no explicit argument here.
            val content = loader.getResourceAsStream(resourcePath)
                ?.bufferedReader()
                ?.readText()
                ?: error(
                    "Contributor '$contributor' asked for resource '$resourcePath', which is not on the " +
                            "classpath. Check it is under src/main/resources of the contributor's module."
                )

            file(path = to, content = content)
        }
    }
}
