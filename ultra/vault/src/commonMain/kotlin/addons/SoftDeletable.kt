package io.peekandpoke.ultra.vault.addons

/**
 * An entity that is deleted by marking it with a [SoftDelete] tombstone instead of removing the row.
 *
 * Soft-delete is a MODEL-LEVEL convention only — Vault applies NO automatic filtering. Every read
 * over a soft-deletable entity must exclude tombstoned rows itself, via the backend DSL helpers
 * (`notDeleted(x.softDelete)` in Karango and Monko). In particular `Repository.findById` returns a
 * soft-deleted entity like any other, and a soft-deleted row still occupies its unique indexes.
 */
interface SoftDeletable {
    /** A [SoftDeletable] that can produce a copy of itself carrying a different marker. */
    interface Mutable<T> : SoftDeletable {
        /** Returns a copy marked with [softDelete]; pass `null` to restore the entity. */
        fun withSoftDelete(softDelete: SoftDelete?): T
    }

    /** The tombstone, or `null` while the entity is alive. */
    val softDelete: SoftDelete?

    /** `true` once a [SoftDelete] marker is set. */
    val isDeleted get() = softDelete != null

    /** `true` while the entity is alive. */
    val isNotDeleted get() = !isDeleted
}
