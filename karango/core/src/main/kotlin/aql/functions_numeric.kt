@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker


/**
 * Return the sine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#sin
 */
@VaultFunctionMarker
fun <T : Number> SIN(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.SIN.numberCall(value)

/**
 * Return the square root of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#sqrt
 */
@VaultFunctionMarker
fun <T : Number> SQRT(
    value: AqlExpression<T>,
): AqlExpression<Number?> =
    AqlFunc.SQRT.nullableNumberCall(value)

/**
 * Return the population standard deviation of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddevpopulation
 */
@VaultFunctionMarker
fun <T : Number> STDDEV_POPULATION(
    value: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.STDDEV_POPULATION.nullableNumberCall(value)

/**
 * Return the sample standard deviation of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddevsample
 */
@VaultFunctionMarker
fun <T : Number> STDDEV_SAMPLE(
    value: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.STDDEV_SAMPLE.nullableNumberCall(value)

/**
 * Return the population standard deviation of the values in array.
 *
 * Alias of STDDEV_POPULATION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddev
 */
@VaultFunctionMarker
fun <T : Number> STDDEV(
    value: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.STDDEV.nullableNumberCall(value)

/**
 * Return the sum of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#stddevsample
 */
@VaultFunctionMarker
fun <T : Number> SUM(
    numArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.SUM.numberCall(numArray)

/**
 * Return the tangent of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#tan
 */
@VaultFunctionMarker
fun <T : Number> TAN(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.TAN.numberCall(value)

/**
 * Return the population variance of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#variancepopulation
 */
@VaultFunctionMarker
fun <T : Number> VARIANCE_POPULATION(
    value: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.VARIANCE_POPULATION.nullableNumberCall(value)

/**
 * Return the sample variance of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#variancesample
 */
@VaultFunctionMarker
fun <T : Number> VARIANCE_SAMPLE(
    value: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.VARIANCE_SAMPLE.nullableNumberCall(value)

/**
 * Return the population variance of the values in array.
 *
 * Alias for VARIANCE_POPULATION
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#variance
 */
@VaultFunctionMarker
fun <T : Number> VARIANCE(
    value: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.VARIANCE.nullableNumberCall(value)
