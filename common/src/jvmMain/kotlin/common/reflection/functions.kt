package de.peekandpoke.ultra.common.reflection

import kotlin.reflect.KClass
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

/**
 * Gets the name of the n'th parameter
 *
 * Notice: the first parameter has index 0
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalReflectionOnLambdas::class)
fun <R, T : Function<R>> T.nthParamName(n: Int): String {

    // Getting the parameters is quite expensive, so we cache it
    return NthParamNameCache.getOrPut(Pair(this::class, n)) {
        val params = this.reflect()?.parameters

        params?.get(n)?.name ?: "param$n"
    }
}

/**
 * Internal cache for parameter names.
 *
 * Due to internal details of the kotlin runtime get parameter names from closure is expensive.
 *
 * Used by [nthParamName]
 */
internal val NthParamNameCache = mutableMapOf<Pair<KClass<*>, Int>, String>()
