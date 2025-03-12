@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> StatementBuilder.INSERT(what: Expression<T>) = InsertExpression(what)

@VaultTerminalExpressionMarker
@JvmName("INSERT_Storable")
@Suppress("UNCHECKED_CAST")
fun <T : Any> StatementBuilder.INSERT(what: Expression<Storable<T>>) = INSERT(what as Expression<T>)

@VaultDslMarker
class InsertExpression<T : Any> internal constructor(private val what: Expression<T>) {
    @Suppress("UNCHECKED_CAST")
    @VaultTerminalExpressionMarker
    infix fun INTO(repo: Repository<in T>): TerminalExpr<T> = InsertExpressionInto(what, repo as Repository<T>)
}

internal class InsertExpressionInto<T : Any>(
    private val expression: Expression<T>,
    private val repo: Repository<T>,
) : TerminalExpr<T> {

    override fun innerType() = expression.getType()

    override fun getType() = repo.getType()

    override fun print(p: Printer) = with(p) {

        append("INSERT ").append(expression).append(" INTO ").append(repo).appendLine()
        append("RETURN NEW").appendLine()
    }
}

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> StatementBuilder.INSERT(entity: Storable<T>) = InsertNewStorable(entity)

class InsertNewStorable<T> internal constructor(val entity: Storable<T>)

@VaultTerminalExpressionMarker
infix fun <T : Any, X : T> InsertNewStorable<X>.INTO(repo: Repository<T>): TerminalExpr<T> =
    InsertNewStorableInto(entity, repo)

internal class InsertNewStorableInto<T : Any, X : T>(private val new: Storable<X>, private val repo: Repository<T>) :
    TerminalExpr<T> {

    override fun innerType(): TypeRef<T> = repo.storedType

    override fun getType() = repo.getType()

    override fun print(p: Printer) = with(p) {

        append("INSERT ").value("v", new).append(" INTO ").append(repo).appendLine()
        append("RETURN NEW").appendLine()
    }
}
