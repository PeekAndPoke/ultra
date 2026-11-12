package io.peekandpoke.ultra.depot.domain

/** What comparing a file with the hash stored at its write found. */
enum class DepotVerification {
    /** The content is what was written. */
    Intact,

    /** The content differs from what was written, or belongs to other metadata. */
    Damaged,

    /** No hash is stored: a file from before hashes were stored, or one the depot did not write. */
    NoHash,

    /** A keyed hash is stored, and the key that made it was erased: nothing can be said. */
    Unverifiable,
}
