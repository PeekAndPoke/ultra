@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

/**
 * Inserts or updates a document.
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T> StatementBuilder.UPSERT(entity: Storable<T>, mode: UpsertMode = UpsertMode.Update) = UpsertPartial(entity, mode)

/**
 * Inserts or updates a document by using the UPDATE variant.
 *
 * See https://www.arangodb.com/docs/stable/aql/operations-upsert.html
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> StatementBuilder.UPSERT_UPDATE(entity: Storable<T>) = UPSERT(entity, UpsertMode.Update)

/**
 * Inserts or updates a document by using the "REPLACE" variant.
 *
 * See https://www.arangodb.com/docs/stable/aql/operations-upsert.html
 */
@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> StatementBuilder.UPSERT_REPLACE(entity: Storable<T>) = UPSERT(entity, UpsertMode.Replace)

/**
 * The mode to used for updating documents.
 *
 * See https://www.arangodb.com/docs/stable/aql/operations-upsert.html
 */
enum class UpsertMode {
    Update,
    Replace
}

class UpsertPartial<T> internal constructor(
    val entity: Storable<T>,
    internal val mode: UpsertMode,
)

@VaultTerminalExpressionMarker
infix fun <T : Any, X : T> UpsertPartial<X>.INTO(repo: Repository<T>): TerminalExpr<T> = UpsertInto(
    entity = entity, repo = repo, mode = mode
)

internal class UpsertInto<T : Any, X : T>(
    private val entity: Storable<X>,
    private val repo: Repository<T>,
    private val mode: UpsertMode,
) : TerminalExpr<T> {

    override fun innerType(): TypeRef<T> = repo.storedType

    override fun getType() = repo.getType()

    override fun print(p: Printer) {

        with(p) {
            append("UPSERT { _key: \"").append(entity._key).append("\" }").appendLine()
            indent {
                append("INSERT ").value("v", entity).appendLine()
                append("${mode.name.uppercase()} ").value("v", entity).append(" IN ").append(repo).appendLine()
                append("RETURN NEW").appendLine()
            }
        }
    }
}
