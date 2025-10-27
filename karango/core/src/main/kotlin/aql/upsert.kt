@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.karango.vault.KarangoRepository
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

/**
 * Inserts or updates a document.
 */
@Suppress("unused", "UnusedReceiverParameter")
@VaultTerminalExpressionMarker
fun <T> AqlStatementBuilder.UPSERT(entity: Storable<T>, mode: AqlUpsertMode = AqlUpsertMode.Update) =
    AqlUpsertPartial(entity, mode)

/**
 * Inserts or updates a document by using the UPDATE variant.
 *
 * See https://www.arangodb.com/docs/stable/aql/operations-upsert.html
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> AqlStatementBuilder.UPSERT_UPDATE(entity: Storable<T>) = UPSERT(entity, AqlUpsertMode.Update)

/**
 * Inserts or updates a document by using the "REPLACE" variant.
 *
 * See https://www.arangodb.com/docs/stable/aql/operations-upsert.html
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> AqlStatementBuilder.UPSERT_REPLACE(entity: Storable<T>) = UPSERT(entity, AqlUpsertMode.Replace)

/**
 * The mode to used for updating documents.
 *
 * See https://www.arangodb.com/docs/stable/aql/operations-upsert.html
 */
enum class AqlUpsertMode {
    Update,
    Replace
}

class AqlUpsertPartial<T> internal constructor(
    val entity: Storable<T>,
    internal val mode: AqlUpsertMode,
)

@VaultTerminalExpressionMarker
infix fun <T : Any, X : T> AqlUpsertPartial<X>.INTO(repo: KarangoRepository<T>): AqlTerminalExpr<T> =
    AqlUpsertIntoExpr(entity = entity, repo = repo, mode = mode)

internal class AqlUpsertIntoExpr<T : Any, X : T>(
    private val entity: Storable<X>,
    private val repo: KarangoRepository<T>,
    private val mode: AqlUpsertMode,
) : AqlTerminalExpr<T> {

    override fun innerType(): TypeRef<T> = repo.storedType

    override fun getType() = repo.getType()

    override fun print(p: AqlPrinter) {
        p.append("UPSERT { _key: \"").append(entity._key).append("\" }").appendLine()

        p.indent {
            append("INSERT ").value("v", entity).appendLine()
            append("${mode.name.uppercase()} ").value("v", entity).append(" IN ").append(repo).appendLine()
            append("RETURN NEW").appendLine()
        }
    }
}
