@file:Suppress("PrivatePropertyName", "detekt:ConstructorParameterNaming")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.VaultTypeConversionMarker

/**
 * Base for all AQL expressions
 */
interface AqlExpression<T> : Expression<T> {
    fun print(p: AqlPrinter)
}

/**
 * An [AqlExpression] representing iterable values
 */
data class AqlIterableExpr<T>(
    private val __name__: String,
    private val __inner__: AqlExpression<List<T>>,
) : AqlExpression<T> {

    override fun getType(): TypeRef<T> = __inner__.getType().unList

    override fun print(p: AqlPrinter) {
        p.name(__name__)
    }

    /**
     * Down-casts the expression of type [T] to the parent type [D]
     */
    @Suppress("UNCHECKED_CAST")
    override fun <D, T : D> downcast(): AqlIterableExpr<D> = this as AqlIterableExpr<D>
}

/**
 * An [AqlExpression] representing a name, like a variable or similar
 */
class AqlNameExpr<T>(private val name: String, private val type: TypeRef<T>) : AqlExpression<T> {
    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: AqlPrinter) {
        p.name(name)
    }
}

/**
 * An [AqlExpression] holding a [value] of [type] that is programmatically given
 *
 * The value optionally can have a [name]. This name is then used for creating the query.
 */
data class AqlValueExpr<T>(
    private val type: TypeRef<T>,
    private val value: T,
    private val name: String = "v",
) : AqlExpression<T> {
    companion object {
        @Suppress("FunctionName")
        fun Null(name: String = "v") = AqlValueExpr(
            type = TypeRef.AnyNull,
            value = null,
            name = name,
        )
    }

    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: AqlPrinter) {
        p.value(name, value as Any?)
    }
}

/**
 * Base interface for all terminal Expressions.
 *
 * A terminal expression can be used to create a query result cursor.
 *
 * A terminal expression ALWAYS is a list type. This is how ArangoDB returns data.
 * The returned data is always an array.
 */
interface AqlTerminalExpr<T> : AqlExpression<List<T>> {
    /**
     * The T within the List<T>. This type is finally used for un-serialization..
     */
    fun innerType(): TypeRef<T> = getType().unList
}

/**
 * Casts the expression to another type
 *
 * Sometimes it might be necessary to change the type of the expression
 */
@Suppress("FunctionName")
@VaultTypeConversionMarker
inline fun <reified R : Any> AqlTerminalExpr<*>.AS(type: TypeRef<List<R>>): AqlTerminalExpr<R> =
    AqlTerminalTypeCastExpression(type, this)

/**
 * Internal expression representing a type cast
 */
class AqlTerminalTypeCastExpression<T>(private val type: TypeRef<List<T>>, private val wrapped: AqlTerminalExpr<*>) :
    AqlTerminalExpr<T> {

    override fun innerType(): TypeRef<T> = type.unList

    override fun getType() = type

    override fun print(p: AqlPrinter) {
        wrapped.print(p)
    }
}

/**
 * An [AqlExpression] representing an array of values
 */
data class AqlArrayValueExpr<T>(
    private val type: TypeRef<List<T>>,
    val items: List<AqlExpression<out T>>,
) : AqlExpression<List<T>> {
    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: AqlPrinter) {
        p.append(this)
    }
}

/**
 * Represents an [AqlExpression] that holds key-value-pairs
 */
class AqlObjectValueExpr<T>(
    private val type: TypeRef<Map<String, T>>,
    val pairs: List<Pair<AqlExpression<String>, AqlExpression<out T>>>,
) : AqlExpression<Map<String, T>> {

    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: AqlPrinter) {
        p.append(this)
    }
}

/**
 * Internal expression impl holding the entire query
 */
internal class AqlRootExpression<T>(
    val builder: AqlStatementBuilder,
    private val ret: AqlTerminalExpr<T>,
) : AqlTerminalExpr<T> {

    private val stmts: List<AqlStatement> get() = builder.stmts

    override fun getType() = ret.getType()

    override fun innerType() = ret.innerType()

    override fun print(p: AqlPrinter) {
        p.append(stmts).append(ret)
    }

    companion object {
        fun <T> from(builderFun: AqlStatementBuilderImpl.() -> AqlTerminalExpr<T>): AqlRootExpression<T> {

            val builder = AqlStatementBuilderImpl()
            val result: AqlTerminalExpr<T> = builder.builderFun()

            return AqlRootExpression(builder, result)
        }
    }
}
