package de.peekandpoke.karango.addons.softdelete

import de.peekandpoke.karango.Karango
import de.peekandpoke.karango.aql.AND
import de.peekandpoke.karango.aql.AqlExpression
import de.peekandpoke.karango.aql.AqlForLoop
import de.peekandpoke.karango.aql.AqlIterableExpr
import de.peekandpoke.karango.aql.AqlPropertyPath
import de.peekandpoke.karango.aql.GTE
import de.peekandpoke.karango.aql.IS_NULL
import de.peekandpoke.karango.aql.NOT
import de.peekandpoke.karango.aql.anyOrTrueIfEmpty
import de.peekandpoke.karango.aql.ts
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.model.search.PagedSearchFilter
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.lang.Expression
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

    fun Expression<T>.getSoftDeleteField(): AqlPropertyPath<SoftDelete, SoftDelete> {
        return this.property(SoftDeletable::softDelete.name)
    }

    fun Expression<T>.getSoftDeletedAt(): AqlPropertyPath<MpInstant, Long> {
        return getSoftDeleteField().deletedAt.ts
    }

    fun AqlForLoop.filterSoftDelete(
        expr: AqlIterableExpr<T>,
        showDeleted: Boolean = false,
        showDeletedAfter: MpInstant? = null,
    ) {
        val conditions = mutableListOf<AqlExpression<Boolean>>()

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
            conditions.anyOrTrueIfEmpty
        )
    }

    fun AqlForLoop.filterSoftDelete(
        expr: AqlIterableExpr<T>,
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
