package de.peekandpoke.karango.aql

import de.peekandpoke.karango.AqlQueryOptionProvider
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.Statement
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.TerminalTypeCastExpression
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import de.peekandpoke.ultra.vault.lang.VaultTypeConversionMarker

/**
 * Casts the expression to another type
 *
 * Sometimes it might be necessary to change the type of the expression
 */
@Suppress("FunctionName")
@VaultTypeConversionMarker
inline fun <reified R : Any> TerminalExpr<*>.AS(type: TypeRef<List<R>>): TerminalExpr<R> =
    TerminalTypeCastExpression(type, this)

/**
 * Internal expression impl holding the entire query
 */
internal class RootExpression<T>(
    val builder: AqlBuilder,
    private val ret: TerminalExpr<T>,
) : TerminalExpr<T> {

    private val stmts: List<Statement> get() = builder.stmts

    override fun getType() = ret.getType()

    override fun innerType() = ret.innerType()

    override fun print(p: Printer) = p.append(stmts).append(ret)

    companion object {
        fun <T> from(builderFun: AqlBuilder.() -> TerminalExpr<T>): RootExpression<T> {

            val builder = AqlBuilder()
            val result: TerminalExpr<T> = builder.builderFun()

            return RootExpression(builder, result)
        }
    }
}

@VaultDslMarker
class AqlBuilder internal constructor() : StatementBuilder {

    private val _optionProviders = mutableListOf<AqlQueryOptionProvider>()

    val queryOptions
        get(): AqlQueryOptionProvider = {
            _optionProviders.fold(it) { acc, next -> next(acc) }
        }

    override val stmts = mutableListOf<Statement>()

    fun queryOptions(block: AqlQueryOptionProvider) {
        _optionProviders.add(block)
    }
}

interface StatementBuilder {

    val stmts: MutableList<Statement>

    fun <T : Statement> T.addStmt(): T = apply { stmts.add(this) }
}
