package de.peekandpoke.karango.addons.softdelete

import de.peekandpoke.karango.Karango
import de.peekandpoke.karango.aql.AND
import de.peekandpoke.karango.aql.ForLoop
import de.peekandpoke.karango.aql.GTE
import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.NOT
import de.peekandpoke.karango.aql.any
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.model.search.PagedSearchFilter
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.IterableExpr
import de.peekandpoke.ultra.vault.lang.PropertyPath
import de.peekandpoke.ultra.vault.lang.property
import de.peekandpoke.ultra.vault.slumber.ts
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

interface SoftDeletableRepositoryAddon<T : SoftDeletable.Mutable<T>> : Repository<T> {

    val kronos: Kronos

    @Suppress("unused")
    @Karango
    data class TriggerKarango(
        val softDelete: SoftDelete,
    )

    suspend fun softDelete(entity: Stored<T>) = save(entity) {
        it.withSoftDelete(
            SoftDelete(
                deletedAt = kronos.instantNow()
            )
        )
    }

    suspend fun softRestore(entity: Stored<T>) = save(entity) {
        it.withSoftDelete(
            null,
        )
    }

    fun Expression<T>.getSoftDeleteField(): PropertyPath<SoftDelete, SoftDelete> {
        return this.property(SoftDeletable::softDelete.name)
    }

    fun Expression<T>.getSoftDeletedAt(): PropertyPath<MpInstant, Long> {
        return getSoftDeleteField().deletedAt.ts
    }

    fun ForLoop.filterSoftDelete(
        expr: IterableExpr<T>,
        showDeleted: Boolean = false,
        showDeletedAfter: MpInstant? = null,
    ) {
        val conditions = mutableListOf<Expression<Boolean>>()

        if (!showDeleted) {
            conditions.add(
                IS_NULL(expr.getSoftDeletedAt())
            )
        }

        if (showDeletedAfter != null) {
            conditions.add(
                NOT(IS_NULL(expr.getSoftDeletedAt())) AND
                        (expr.getSoftDeletedAt() GTE showDeletedAfter.toEpochMillis()),
            )
        }

        FILTER(
            conditions.any
        )
    }

    fun ForLoop.filterSoftDelete(
        expr: IterableExpr<T>,
        filter: PagedSearchFilter,
        showRecentlyDeletedFor: Duration = 24.hours,
    ) {
        filterSoftDelete(
            expr = expr,
            showDeleted = filter.hasIncludeDeletedOption(),
            showDeletedAfter = if (filter.hasIncludeRecentlyDeletedOption()) {
                kronos.instantNow().minus(showRecentlyDeletedFor)
            } else {
                null
            }
        )
    }

    fun IndexBuilder.PersistentIndexBuilder<T>.indexSoftDelete() {
        field { getSoftDeletedAt() }
    }
}
