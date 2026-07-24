package io.peekandpoke.monko.lang.dsl

import com.mongodb.client.model.Filters
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.vault.addons.SoftDelete
import org.bson.conversions.Bson

/**
 * Mongo filter: the `SoftDeletable` row is NOT soft-deleted — its [SoftDelete] marker is null (or
 * missing; `eq null` matches both).
 *
 * Vault's soft-delete is a model-level convention (no repository auto-filtering), so queries over a
 * `SoftDeletable` entity must exclude deleted rows explicitly:
 *
 * ```
 * find { r -> filter(and(r.userId eq userId, notDeleted(r.softDelete))) }
 * ```
 */
fun notDeleted(softDelete: MongoPropertyPath<*, SoftDelete>): Bson = Filters.eq(softDelete.toFieldPath(), null)
