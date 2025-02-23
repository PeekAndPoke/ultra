package de.peekandpoke.ultra.vault.lang

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType

typealias PathExpr<P> = PropertyPath<P, P>

@Suppress("unused")
data class PropertyPath<P, T>(
    val previous: PropertyPath<*, *>?,
    val current: Step<T>,
) : Expression<T> {

    companion object {
        fun <T> start(root: Expression<T>) = PropertyPath<T, T>(null, Step.Expr(root))
    }

    sealed class Step<T>(private val type: TypeRef<T>) : Expression<T> {
        override fun getType() = type

        open fun print(p: Printer, isRoot: Boolean) = print(p)

        class Prop<T>(private val name: String, type: TypeRef<T>) : Step<T>(type) {

            override fun print(p: Printer, isRoot: Boolean) {
                if (!isRoot) {
                    p.append(".")
                }
                print(p)
            }

            override fun print(p: Printer) = p.name(name)
        }

        class Expr<T>(private val expr: Expression<T>) : Step<T>(expr.getType()) {
            override fun print(p: Printer) = p.append(expr)
        }

        class Operation<T>(private val op: String, type: TypeRef<T>) : Step<T>(type) {
            override fun print(p: Printer) = p.append(op)
        }
    }


    override fun getType() = current.getType()

    override fun print(p: Printer) {

        previous?.apply { p.append(this) }

        current.print(p = p, isRoot = previous == null)
    }

    inline fun <reified NT> property(prop: String) = PropertyPath<P, NT>(this, Step.Prop(prop, kType()))

    inline fun <NF, reified NT> append(prop: String) = PropertyPath<NF, NT>(this, Step.Prop(prop, kType()))

    fun dropRoot(): PropertyPath<P, T>? {
        val result = when (previous) {
            null -> null
            else -> PropertyPath<P, T>(
                previous = previous.dropRoot(),
                current = current,
            )
        }

        return result
    }
}

//// generic property

inline fun <reified T> Expression<*>.property(name: String): PropertyPath<T, T> {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        // Is this already a Property Path Expression. Then we follow this one.
        is PathExpr<*> -> property<String>(name) as PropertyPath<T, T>

        // Else we start a property path
        else -> PropertyPath.start(this).append<T, T>(name)
    }
}
