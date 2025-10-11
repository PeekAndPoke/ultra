@file:Suppress("ObjectPropertyName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.L2
import de.peekandpoke.ultra.vault.lang.PropertyPath

private fun <P, T, NP> PropertyPath<P, T>.internalExpand(): PropertyPath<NP, T> {
    return PropertyPath(this, PropertyPath.Step.Operation("[*]", current.getType()))
}

@JvmName("expandList")
fun <F, L : List<F>, T> PropertyPath<L, List<T>>.expand(): PropertyPath<F, List<T>> {
    return internalExpand()
}

@JvmName("expandSet")
fun <F, L : Set<F>, T> PropertyPath<L, Set<T>>.expand(): PropertyPath<F, List<T>> {
    @Suppress("UNCHECKED_CAST")
    return (this as PropertyPath<List<F>, List<T>>).internalExpand()
}

inline fun <reified T> Expression<List<T>>.expand(): PropertyPath<T, List<T>> {
    return PropertyPath.start(this).expand()
}

fun <F, T> PropertyPath<F, L2<T>>.contract(): PropertyPath<F, List<T>> {
    return PropertyPath(
        this,
        PropertyPath.Step.Operation("[**]", current.getType().unList)
    )
}
