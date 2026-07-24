package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.vault.addons.SoftDelete

/**
 * AQL predicate: the `SoftDeletable` row is NOT soft-deleted — its [SoftDelete] marker is null.
 *
 * Vault's soft-delete is a model-level convention (no repository auto-filtering), so queries over a
 * `SoftDeletable` entity must exclude deleted rows explicitly:
 *
 * ```
 * FOR(repo) { entity ->
 *     FILTER(notDeleted(entity.softDelete))
 *     RETURN(entity)
 * }
 * ```
 */
fun notDeleted(softDelete: AqlExpression<SoftDelete>): AqlExpression<Boolean> = IS_NULL(softDelete)
