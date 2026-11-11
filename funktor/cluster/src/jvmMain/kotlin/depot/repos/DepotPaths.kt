package io.peekandpoke.funktor.cluster.depot.repos

/** Thrown for a path a depot repository refuses, one that could leave the repository (ULTRA-00304). */
class DepotInvalidPathException(val path: String) : IllegalArgumentException("Invalid depot path")

/**
 * Checks a path within a depot repository and returns it: a `.` or `..` segment, a backslash or a NUL throws
 * [DepotInvalidPathException]. Every repository refuses the same paths (ULTRA-00304).
 */
fun String.validateDepotPath(): String = apply {
    val invalid = contains('\\') || contains(Char(0)) || split('/').any { it == ".." || it == "." }

    if (invalid) throw DepotInvalidPathException(this)
}
