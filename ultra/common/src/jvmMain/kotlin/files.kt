package io.peekandpoke.ultra.common

import java.io.File

/**
 * Ensures this file path exists as a directory, creating it and any parent directories if necessary.
 *
 * Does nothing when the path already exists as a regular file, and does not report a failed
 * `mkdirs()` - check `isDirectory` yourself when the outcome matters.
 *
 * @return this [File] instance for chaining.
 */
fun File.ensureDirectory() = apply {
    if (!exists()) {
        mkdirs()
    }
}

/**
 * Deletes this directory and all its contents, then recreates it as an empty directory.
 *
 * The recursive delete descends into symlinked sub-directories, so it can remove files outside of
 * this directory. Deletion is best-effort: a failure to remove single entries is not reported.
 *
 * @return this [File] instance for chaining.
 */
fun File.cleanDirectory() = apply {
    deleteRecursively()
    ensureDirectory()
}

/**
 * Returns a new [File] representing the given [child] path resolved relative to this directory.
 *
 * The [child] path is taken verbatim: `..` segments are kept, so the result can point outside of
 * this directory. Validate [child] before passing anything caller-supplied.
 */
fun File.child(child: File) = File(this, child.path)
