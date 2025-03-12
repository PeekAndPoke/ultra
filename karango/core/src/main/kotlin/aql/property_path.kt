@file:Suppress("ObjectPropertyName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.L2
import de.peekandpoke.ultra.vault.lang.PropertyPath

interface ArrayExpansion

@Suppress("ClassName", "DANGEROUS_CHARACTERS")
object `*` : ArrayExpansion

interface ArrayContraction

@Suppress("ClassName", "DANGEROUS_CHARACTERS")
object `**` : ArrayContraction

private fun <P, T, NP> PropertyPath<P, T>.internalExpand(): PropertyPath<NP, T> {
    return PropertyPath<NP, T>(this, PropertyPath.Step.Operation("[*]", current.getType()))
}

@Suppress("UNUSED_PARAMETER", "DANGEROUS_CHARACTERS")
@Deprecated(
    message = "Use expand() instead",
    replaceWith = ReplaceWith(".expand()"),
    level = DeprecationLevel.WARNING
)
operator fun <F, L : List<F>, T> PropertyPath<L, List<T>>.get(`*`: ArrayExpansion): PropertyPath<F, List<T>> {
    return expand()
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

@Suppress("UNUSED_PARAMETER", "DANGEROUS_CHARACTERS")
inline operator fun <reified T> Expression<List<T>>.get(`*`: ArrayExpansion): PropertyPath<T, List<T>> {
    return PropertyPath.start(this).expand()
}

fun <F, T> PropertyPath<F, L2<T>>.contract(): PropertyPath<F, List<T>> {
    return PropertyPath(
        this,
        PropertyPath.Step.Operation("[**]", current.getType().unList)
    )
}

@Suppress("UNUSED_PARAMETER", "DANGEROUS_CHARACTERS")
@Deprecated(
    message = "Use contract() instead",
    replaceWith = ReplaceWith(".contract()"),
    level = DeprecationLevel.WARNING
)
operator fun <F, T> PropertyPath<F, L2<T>>.get(`**`: ArrayContraction): PropertyPath<F, List<T>> {
    return contract()
}
