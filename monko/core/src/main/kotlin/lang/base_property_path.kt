@file:Suppress("ObjectPropertyName")

package de.peekandpoke.monko.lang

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType

typealias MongoPathExpr<P> = MongoPropertyPath<P, P>

@Suppress("unused")
data class MongoPropertyPath<P, T>(
    val previous: MongoPropertyPath<*, *>?,
    val current: Step<T>,
) : MongoExpression<T> {

    companion object {
        fun <T> start(root: MongoExpression<T>) = MongoPropertyPath<T, T>(null, Step.Expr(root))
    }

    sealed class Step<T>(private val type: TypeRef<T>) : MongoExpression<T> {
        override fun getType() = type

        open fun print(p: MongoPrinter, isRoot: Boolean) = print(p)

        class Prop<T>(private val name: String, type: TypeRef<T>) : Step<T>(type) {

            override fun print(p: MongoPrinter, isRoot: Boolean) {
                if (!isRoot) {
                    p.append(".")
                }
                print(p)
            }

            override fun print(p: MongoPrinter) {
                p.name(name)
            }
        }

        class Expr<T>(private val expr: MongoExpression<T>) : Step<T>(expr.getType()) {
            override fun print(p: MongoPrinter) {
                p.append(expr)
            }
        }

        class Operation<T>(private val op: String, type: TypeRef<T>) : Step<T>(type) {
            override fun print(p: MongoPrinter) {
                p.append(op)
            }
        }
    }

    override fun getType() = current.getType()

    override fun print(p: MongoPrinter) {

        previous?.apply { p.append(this) }

        current.print(p = p, isRoot = previous == null)
    }

    inline fun <reified NT> property(prop: String) =
        MongoPropertyPath<P, NT>(this, Step.Prop(prop, kType()))

    inline fun <NF, reified NT> append(prop: String) =
        MongoPropertyPath<NF, NT>(this, Step.Prop(prop, kType()))

    fun getAsList(): List<MongoExpression<*>> {
        val result = mutableListOf<MongoExpression<*>>()

        var current: MongoPropertyPath<*, *>? = this

        while (current != null) {
            result.add(0, current.current)
            current = current.previous
        }

        return result
    }

    fun getRoot(): MongoExpression<*>? = when (val p = previous) {
        null -> null
        else -> p.getRoot()
    }

    fun dropRoot(): MongoPropertyPath<P, T>? {
        val result = when (previous) {
            null -> null
            else -> MongoPropertyPath<P, T>(
                previous = previous.dropRoot(),
                current = current,
            )
        }

        return result
    }
}

// // generic property

inline fun <reified T> MongoExpression<*>.property(name: String): MongoPropertyPath<T, T> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        // Is this already a Property Path Expression. Then we follow this one.
        is MongoPathExpr<*> -> property<String>(name) as MongoPropertyPath<T, T>

        // Else we start a property path
        else -> MongoPropertyPath.start(this).append<T, T>(name)
    }
}
