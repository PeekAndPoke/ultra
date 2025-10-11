package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.unList

/**
 * Base interface for all Expressions.
 *
 * Expression return something of a specific type T. Expressions can be nested within other Expressions.
 */
interface Expression<T> : Printable {
    /**
     * Returns a reference to the type that the expression represents.
     *
     * The type information is needed for un-serializing a query result.
     */
    fun getType(): TypeRef<T>

    /**
     * Up-casts the expression of type [T] to the child type [U]
     */
    @Suppress("UNCHECKED_CAST")
    fun <U : T> upcastTo(): Expression<U> = this as Expression<U>

    /**
     * Down-casts the expression of type [T] to the parent type [D]
     */
    @Suppress("UNCHECKED_CAST")
    fun <D, T : D> downcast(): Expression<D> = this as Expression<D>

    /**
     * Casts the expression
     */
    @Suppress("UNCHECKED_CAST")
    fun <U> forceCastTo(): Expression<U> = this as Expression<U>

    /**
     * Makes the expression nullable
     */
    @Suppress("UNCHECKED_CAST")
    fun nullable(): Expression<T?> = this as Expression<T?>
}

/**
 * An [Expression] representing a name, like a variable or similar
 */
class NameExpr<T>(private val name: String, private val type: TypeRef<T>) : Expression<T> {
    /**
     * The type that is represented by the expression
     */
    override fun getType() = type

    override fun print(p: Printer) = p.name(name)
}

/**
 * An [Expression] holding a [value] of [type] that is programmatically given
 *
 * The value optionally can have a [name]. This name is then used for creating the query.
 */
data class ValueExpr<T>(private val type: TypeRef<T>, private val value: T, private val name: String = "v") :
    Expression<T> {

    override fun getType() = type
    override fun print(p: Printer) = p.value(name, value as Any)
}

/**
 * An [Expression] representing an array of values
 */
data class ArrayValueExpr<T>(private val type: TypeRef<List<T>>, val items: List<Expression<out T>>) :
    Expression<List<T>> {

    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: Printer) = p.append(this)
}

/**
 * An [Expression] representing iterable values
 */
data class IterableExpr<T>(private val __name__: String, private val __inner__: Expression<List<T>>) : Expression<T> {

    override fun getType(): TypeRef<T> = __inner__.getType().unList

    override fun print(p: Printer) = p.name(__name__)

    /**
     * Down-casts the expression of type [T] to the parent type [D]
     */
    @Suppress("UNCHECKED_CAST")
    override fun <D, T : D> downcast(): IterableExpr<D> = this as IterableExpr<D>
}

/**
 * Represents an [Expression] that holds a null
 *
 * Can optionally have a [name] that is used when printing the query.
 */
class NullValueExpr(val name: String = "v") : Expression<Any?> {

    /** The type of the expression */
    override fun getType() = TypeRef.AnyNull

    /** Prints the expression */
    override fun print(p: Printer) = p.value(name, null)
}

/**
 * Represents an [Expression] that holds key-value-pairs
 */
class ObjectValueExpr<T>(
    private val type: TypeRef<Map<String, T>>,
    val pairs: List<Pair<Expression<String>, Expression<out T>>>,
) : Expression<Map<String, T>> {

    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: Printer) = p.append(this)
}

/**
 * Base interface for all terminal Expressions.
 *
 * A terminal expression can be used to create a query result cursor.
 *
 * A terminal expression ALWAYS is a list type. This is how ArangoDB returns data.
 * The returned data is always an array.
 */
interface TerminalExpr<T> : Expression<List<T>> {
    /**
     * The T within the List<T>. This type is finally used for un-serialization..
     */
    fun innerType(): TypeRef<T>
}
