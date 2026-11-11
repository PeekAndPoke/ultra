package io.peekandpoke.ultra.depot.repos

/**
 * Thrown for a path a depot repository refuses, one that could leave the repository (ULTRA-00304). It does not carry
 * the path: that is caller input, and a message or log line built from it would leak or inject it.
 */
class DepotInvalidPathException : IllegalArgumentException("Invalid depot path")

/**
 * Checks a path within a depot repository and returns it: a `.` or `..` segment (between `/` or `\`, the Windows
 * separator), a NUL or the reserved first segment [DEPOT_META_DIR] throws [DepotInvalidPathException]. Every
 * repository refuses the same paths (ULTRA-00304). A backslash elsewhere is allowed: S3 keys may hold one.
 */
fun String.validateDepotPath(): String = apply {
    val segments = split('/', '\\')
    val invalid = contains(Char(0)) ||
            segments.any { it == ".." || it == "." } ||
            // The file system repository keeps the metadata there
            segments.firstOrNull { it.isNotEmpty() } == DEPOT_META_DIR

    if (invalid) throw DepotInvalidPathException()
}

/** The reserved top-level folder for metadata; no file can be addressed in it, in any repository. */
const val DEPOT_META_DIR = ".meta"
