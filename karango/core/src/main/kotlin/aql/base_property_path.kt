@file:Suppress("ObjectPropertyName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.lang.L2

typealias AqlPathExpr<P> = AqlPropertyPath<P, P>

@Suppress("unused")
data class AqlPropertyPath<P, T>(
    val previous: AqlPropertyPath<*, *>?,
    val current: Step<T>,
) : AqlExpression<T> {

    companion object {
        fun <T> start(root: AqlExpression<T>) = AqlPropertyPath<T, T>(null, Step.Expr(root))
    }

    sealed class Step<T>(private val type: TypeRef<T>) : AqlExpression<T> {
        override fun getType() = type

        open fun print(p: AqlPrinter, isRoot: Boolean) = print(p)

        class Prop<T>(private val name: String, type: TypeRef<T>) : Step<T>(type) {

            override fun print(p: AqlPrinter, isRoot: Boolean) {
                if (!isRoot) {
                    p.append(".")
                }
                print(p)
            }

            override fun print(p: AqlPrinter) {
                p.name(name)
            }
        }

        class Expr<T>(private val expr: AqlExpression<T>) : Step<T>(expr.getType()) {
            override fun print(p: AqlPrinter) {
                p.append(expr)
            }
        }

        class Operation<T>(private val op: String, type: TypeRef<T>) : Step<T>(type) {
            override fun print(p: AqlPrinter) {
                p.append(op)
            }
        }
    }

    override fun getType() = current.getType()

    override fun print(p: AqlPrinter) {

        previous?.apply { p.append(this) }

        current.print(p = p, isRoot = previous == null)
    }

    inline fun <reified NT> property(prop: String) = AqlPropertyPath<P, NT>(this, Step.Prop(prop, kType()))

    inline fun <NF, reified NT> append(prop: String) = AqlPropertyPath<NF, NT>(this, Step.Prop(prop, kType()))

    fun getAsList(): List<AqlExpression<*>> {
        val result = mutableListOf<AqlExpression<*>>()

        var current: AqlPropertyPath<*, *>? = this

        while (current != null) {
            result.add(0, current.current)
            current = current.previous
        }

        return result
    }

    fun getRoot(): AqlExpression<*>? = when (val p = previous) {
        null -> null
        else -> p.getRoot()
    }

    fun dropRoot(): AqlPropertyPath<P, T>? {
        val result = when (previous) {
            null -> null
            else -> AqlPropertyPath<P, T>(
                previous = previous.dropRoot(),
                current = current,
            )
        }

        return result
    }
}

// // generic property

inline fun <reified T> AqlExpression<*>.property(name: String): AqlPropertyPath<T, T> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        // Is this already a Property Path Expression. Then we follow this one.
        is AqlPathExpr<*> -> property<String>(name) as AqlPropertyPath<T, T>

        // Else we start a property path
        else -> AqlPropertyPath.start(this).append<T, T>(name)
    }
}

private fun <P, T, NP> AqlPropertyPath<P, T>.internalExpand(): AqlPropertyPath<NP, T> {
    return AqlPropertyPath(this, AqlPropertyPath.Step.Operation("[*]", current.getType()))
}

@JvmName("expandList")
fun <F, L : List<F>, T> AqlPropertyPath<L, List<T>>.expand(): AqlPropertyPath<F, List<T>> {
    return internalExpand()
}

@JvmName("expandSet")
fun <F, L : Set<F>, T> AqlPropertyPath<L, Set<T>>.expand(): AqlPropertyPath<F, List<T>> {
    @Suppress("UNCHECKED_CAST")
    return (this as AqlPropertyPath<List<F>, List<T>>).internalExpand()
}

inline fun <reified T> AqlExpression<List<T>>.expand(): AqlPropertyPath<T, List<T>> {
    return AqlPropertyPath.start(this).expand()
}

fun <F, T> AqlPropertyPath<F, L2<T>>.contract(): AqlPropertyPath<F, List<T>> {
    return AqlPropertyPath(
        this,
        AqlPropertyPath.Step.Operation("[**]", current.getType().unList)
    )
}
