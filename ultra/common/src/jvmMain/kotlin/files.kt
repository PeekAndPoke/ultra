package io.peekandpoke.ultra.common

import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Ensures this file path exists as a directory, creating it and any parent directories if necessary.
 *
 * @return this [File] instance for chaining.
 * @throws IllegalStateException when the path cannot be made into a directory — because it already
 *   exists as a regular file, or because `mkdirs()` failed. Returning quietly would leave the caller
 *   writing into a directory that is not there.
 */
fun File.ensureDirectory() = apply {
    if (isDirectory) {
        return@apply
    }

    check(!exists()) { "Cannot create directory '$path': a non-directory already exists there" }

    // mkdirs() reports false both on failure and on a concurrent create, so re-check the outcome
    mkdirs()

    check(isDirectory) { "Failed to create directory '$path'" }
}

/**
 * Deletes this directory and all its contents, then recreates it as an empty directory.
 *
 * Symlinks are removed as links and NOT followed, so a link pointing out of this directory cannot
 * take its target with it.
 *
 * @return this [File] instance for chaining.
 * @throws IllegalStateException when the directory cannot be recreated afterwards.
 */
fun File.cleanDirectory() = apply {
    if (exists()) {
        deleteContentsNotFollowingLinks(toPath())
    }

    ensureDirectory()
}

/**
 * Deletes [dir] and everything below it, treating a symlink as a leaf.
 *
 * `File.deleteRecursively` walks into a symlinked directory and deletes its contents, which reaches
 * outside the tree being cleaned. `Files.walkFileTree` does not follow links unless asked to.
 */
private fun deleteContentsNotFollowingLinks(dir: Path) {
    Files.walkFileTree(
        dir,
        object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.deleteIfExists(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir2: Path, exc: IOException?): FileVisitResult {
                Files.deleteIfExists(dir2)
                return FileVisitResult.CONTINUE
            }
        },
    )
}

/**
 * Returns a new [File] representing the given [child] path resolved relative to this directory.
 *
 * @throws IllegalArgumentException when [child] is absolute, or escapes this directory through `..`.
 *   The check is on the normalised path, so `a/../../b` is rejected while `a/../b` is fine.
 */
fun File.child(child: File): File {
    require(!child.isAbsolute) { "Child path must be relative, got '${child.path}'" }

    val base = toPath().normalize()
    val resolved = base.resolve(child.toPath()).normalize()

    require(resolved.startsWith(base)) {
        "Child path must stay inside '$path', got '${child.path}'"
    }

    return resolved.toFile()
}
