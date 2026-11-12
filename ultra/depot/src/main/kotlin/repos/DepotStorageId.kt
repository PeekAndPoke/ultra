package io.peekandpoke.ultra.depot.repos

import java.nio.file.Path

/**
 * The place a repository holds its files in: a [kind] of storage and a [place] in it. With [nests], the place is a
 * path of the file system, and a place inside another one is the same storage.
 */
data class DepotStorageId(val kind: String, val place: String, val nests: Boolean = false) {

    /** Whether the two are one place, or one lies inside the other. */
    fun overlaps(other: DepotStorageId): Boolean = when {
        kind != other.kind -> false
        nests && other.nests -> Path.of(place).let { mine -> Path.of(other.place).let { mine.startsWith(it) || it.startsWith(mine) } }
        else -> place == other.place
    }
}
