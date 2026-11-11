package io.peekandpoke.funktor.cluster.depot.repos

/**
 * Thrown for a path a depot repository refuses, one that could leave the repository (ULTRA-00304). It does not carry
 * the path: that is caller input, and a message or log line built from it would leak or inject it.
 */
class DepotInvalidPathException : IllegalArgumentException("Invalid depot path")

/**
 * Checks a path within a depot repository and returns it: a `.` or `..` segment (between `/` or `\`, the Windows
 * separator) or a NUL throws [DepotInvalidPathException]. Every repository refuses the same paths (ULTRA-00304). A
 * backslash elsewhere is allowed: S3 keys may hold one.
 */
fun String.validateDepotPath(): String = apply {
    val invalid = contains(Char(0)) || split('/', '\\').any { it == ".." || it == "." }

    if (invalid) throw DepotInvalidPathException()
}
