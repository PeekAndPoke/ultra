@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.karango.vault.KarangoRepository
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> AqlStatementBuilder.INSERT(what: AqlExpression<T>) = AqlInsertExpression(what)

@VaultTerminalExpressionMarker
@JvmName("INSERT_Storable")
@Suppress("UNCHECKED_CAST")
fun <T : Any> AqlStatementBuilder.INSERT(what: AqlExpression<Storable<T>>) = INSERT(what as AqlExpression<T>)

@VaultDslMarker
class AqlInsertExpression<T : Any> internal constructor(private val what: AqlExpression<T>) {
    @Suppress("UNCHECKED_CAST")
    @VaultTerminalExpressionMarker
    infix fun INTO(repo: KarangoRepository<in T>): AqlTerminalExpr<T> =
        AqlInsertExpressionInto(expression = what, repo = repo as KarangoRepository<T>)
}

internal class AqlInsertExpressionInto<T : Any>(
    private val expression: AqlExpression<T>,
    private val repo: KarangoRepository<T>,
) : AqlTerminalExpr<T> {

    override fun innerType() = expression.getType()

    override fun getType() = repo.getType()

    override fun print(p: AqlPrinter) {
        p.append("INSERT ").append(expression).append(" INTO ").append(repo).appendLine()
        p.append("RETURN NEW").appendLine()
    }
}

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T : Any> AqlStatementBuilder.INSERT(entity: Storable<T>) = AqlInsertNewStorable(entity)

class AqlInsertNewStorable<T> internal constructor(val entity: Storable<T>)

@VaultTerminalExpressionMarker
infix fun <T : Any, X : T> AqlInsertNewStorable<X>.INTO(repo: KarangoRepository<T>): AqlTerminalExpr<T> =
    AqlInsertNewStorableInto(entity, repo)

internal class AqlInsertNewStorableInto<T : Any, X : T>(
    private val new: Storable<X>,
    private val repo: KarangoRepository<T>,
) : AqlTerminalExpr<T> {

    override fun innerType(): TypeRef<T> = repo.storedType

    override fun getType() = repo.getType()

    override fun print(p: AqlPrinter) {
        p.append("INSERT ").value("v", new).append(" INTO ").append(repo).appendLine()
        p.append("RETURN NEW").appendLine()
    }
}
