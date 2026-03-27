package io.peekandpoke.ultra.common

import java.io.File

/**
 * Ensures this file path exists as a directory, creating it and any parent directories if necessary.
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
 * @return this [File] instance for chaining.
 */
fun File.cleanDirectory() = apply {
    deleteRecursively()
    ensureDirectory()
}

/**
 * Returns a new [File] representing the given [child] path resolved relative to this directory.
 */
fun File.child(child: File) = File(this, child.path)
