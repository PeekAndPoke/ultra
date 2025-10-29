@file:Suppress("PrivatePropertyName", "detekt:ConstructorParameterNaming")

package de.peekandpoke.monko.lang

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.lang.Expression

/**
 * Base for all AQL expressions
 */
interface MongoExpression<T> : Expression<T> {
    fun print(p: MongoPrinter)
}

/**
 * An [MongoIterableExpr] representing iterable values
 */
data class MongoIterableExpr<T>(
    private val __name__: String,
    private val __inner__: MongoExpression<List<T>>,
) : MongoExpression<T> {

    override fun getType(): TypeRef<T> = __inner__.getType().unList

    override fun print(p: MongoPrinter) {
        p.name(__name__)
    }

    /**
     * Down-casts the expression of type [T] to the parent type [D]
     */
    @Suppress("UNCHECKED_CAST")
    override fun <D, T : D> downcast(): MongoIterableExpr<D> = this as MongoIterableExpr<D>
}

/**
 * An [MongoNameExpr] representing a name, like a variable or similar
 */
class MongoNameExpr<T>(private val name: String, private val type: TypeRef<T>) : MongoExpression<T> {
    /** The type of the expression */
    override fun getType() = type

    /** Prints the expression */
    override fun print(p: MongoPrinter) {
        p.name(name)
    }
}
