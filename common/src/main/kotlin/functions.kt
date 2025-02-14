package de.peekandpoke.ultra.common

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

/**
 * Gets the name of the n'th parameter
 *
 * Notice: the first parameter has index 0
 */
@OptIn(ExperimentalReflectionOnLambdas::class)
fun <R, T : Function<R>> T.nthParamName(n: Int): String {

    // Getting the parameters is quite expensive, so we cache it
    return NthParamNameCache.getOrPut(Pair(this::class, n)) {
        val params = when (this) {
            is KFunction<*> -> parameters
            else -> reflect()?.parameters
        }?.filter { it.kind == KParameter.Kind.VALUE }

        params?.getOrNull(n)?.name ?: "p${n + 1}"
    }
}

/**
 * Internal cache for parameter names.
 *
 * Due to internal details of the kotlin runtime get parameter names from closure is expensive.
 *
 * Used by [nthParamName]
 */
private val NthParamNameCache = mutableMapOf<Pair<KClass<*>, Int>, String>()
