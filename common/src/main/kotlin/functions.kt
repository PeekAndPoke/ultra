package de.peekandpoke.ultra.common

import kotlin.reflect.KClass
import kotlin.reflect.jvm.reflect

/**
 * Gets the name of the n'th parameter
 *
 * Notice: the first parameter has index 1
 */
fun <R, T : Function<R>> T.nthParamName(n: Int): String {

    // Getting the parameters is quite expensive, so we cache it
    return NthParamNameCache.getOrPut(Pair(this::class, n)) {
        val params = this.reflect()?.parameters

        params?.get(n)?.name ?: "param$n"
    }
}

internal val NthParamNameCache = mutableMapOf<Pair<KClass<*>, Int>, String>()
