package de.peekandpoke.ultra.common

import kotlin.reflect.jvm.reflect

/**
 * Gets the name of the n'th parameter
 *
 * Notice: the first parameter has index 1
 */
fun <R, T : Function<R>> T.nthParamName(n: Int): String {

    val params = this.reflect()?.parameters

    return params?.get(n)?.name ?: "param$n"
}
