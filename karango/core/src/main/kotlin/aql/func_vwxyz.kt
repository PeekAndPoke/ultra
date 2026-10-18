@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

/**
 * Return the population variance of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#variancepopulation
 */
val VARIANCE_POPULATION = aqlFunc<Number?>("VARIANCE_POPULATION")

/** Return the population variance of the values in array. */
fun <T : Number> VARIANCE_POPULATION(value: AqlExpression<List<T>>): AqlExpression<Number?> =
    VARIANCE_POPULATION.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the sample variance of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#variancesample
 */
val VARIANCE_SAMPLE = aqlFunc<Number?>("VARIANCE_SAMPLE")

/** Return the sample variance of the values in array. */
fun <T : Number> VARIANCE_SAMPLE(value: AqlExpression<List<T>>): AqlExpression<Number?> =
    VARIANCE_SAMPLE.call(value)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Return the population variance of the values in array.
 *
 * Alias for VARIANCE_POPULATION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#variance
 */
val VARIANCE = aqlFunc<Number?>("VARIANCE")

fun <T : Number> VARIANCE(value: AqlExpression<List<T>>): AqlExpression<Number?> =
    VARIANCE.call(value)
