package de.peekandpoke.ultra.vault.addons

interface SoftDeletable {
    interface Mutable<T> : SoftDeletable {
        fun withSoftDelete(softDelete: SoftDelete?): T
    }

    val softDelete: SoftDelete?

    val isDeleted get() = softDelete != null

    val isNotDeleted get() = !isDeleted
}
