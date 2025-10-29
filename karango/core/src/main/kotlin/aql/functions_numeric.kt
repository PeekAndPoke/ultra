@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kListType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker



/**
 * Return the arcsine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#asin
 */
@VaultFunctionMarker
fun <T : Number> ASIN(
    value: AqlExpression<T>,
): AqlExpression<Number?> =
    AqlFunc.ASIN.nullableNumberCall(value)

/**
 * Return the arctangent of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#atan
 */
@VaultFunctionMarker
fun <T : Number> ATAN(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.ATAN.numberCall(value)

/**
 * Return the arctangent of the quotient of y and x.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#atan2
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> ATAN2(
    x: AqlExpression<T1>,
    y: AqlExpression<T2>,
): AqlExpression<Number> =
    AqlFunc.ATAN2.numberCall(x, y)

/**
 * Return the average (arithmetic mean) of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#average
 */
@VaultFunctionMarker
fun <T : Number> AVERAGE(
    numArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.AVERAGE.numberCall(numArray)

/**
 * Return the average (arithmetic mean) of the values in array.
 *
 * Alias of AVERAGE
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#avg
 */
@VaultFunctionMarker
fun <T : Number> AVG(
    numArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.AVG.numberCall(numArray)

/**
 * Return the integer closest but not less than value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#ceil
 */
@VaultFunctionMarker
fun <T : Number> CEIL(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.CEIL.numberCall(value)

/**
 * Return the cosine of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#cos
 */
@VaultFunctionMarker
fun <T : Number> COS(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.COS.numberCall(value)

/**
 * Return the angle converted from radians to degrees.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#degrees
 */
@VaultFunctionMarker
fun <T : Number> DEGREES(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.DEGREES.numberCall(value)

/**
 * Return Euler's constant (2.71828...) raised to the power of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#exp
 */
@VaultFunctionMarker
fun <T : Number> EXP(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.EXP.numberCall(value)

/**
 * Return 2 raised to the power of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#exp2
 */
@VaultFunctionMarker
fun <T : Number> EXP2(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.EXP2.numberCall(value)

/**
 * Return the integer closest but not greater than value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#floor
 */
@VaultFunctionMarker
fun <T : Number> FLOOR(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.FLOOR.numberCall(value)

/**
 * Return the natural logarithm of value. The base is Euler's constant (2.71828...).
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker
fun <T : Number> LOG(
    value: AqlExpression<T>,
): AqlExpression<Number?> =
    AqlFunc.LOG.nullableNumberCall(value)

/**
 * Return the base 2 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker
fun <T : Number> LOG2(
    value: AqlExpression<T>,
): AqlExpression<Number?> =
    AqlFunc.LOG2.nullableNumberCall(value)

/**
 * Return the base 10 logarithm of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#log
 */
@VaultFunctionMarker
fun <T : Number> LOG10(
    value: AqlExpression<T>,
): AqlExpression<Number?> =
    AqlFunc.LOG10.nullableNumberCall(value)

/**
 * Return the greatest element of anyArray. The array is not limited to numbers. Also see type and value order.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#min
 */
@VaultFunctionMarker
fun <T : Any> MAX(
    array: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.MAX.nullableNumberCall(array)

/**
 * Return the median value of the values in array.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#median
 */
@VaultFunctionMarker
fun <T : Number> MEDIAN(
    numArray: AqlExpression<List<T>>,
): AqlExpression<Number> =
    AqlFunc.MEDIAN.numberCall(numArray)

/**
 * Return the smallest element of anyArray. The array is not limited to numbers. Also see type and value order.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#min
 */
@VaultFunctionMarker
fun <T : Number> MIN(
    numArray: AqlExpression<List<T>>,
): AqlExpression<Number?> =
    AqlFunc.MIN.nullableNumberCall(numArray)

/**
 * Return the nth percentile of the values in numArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#percentile
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> PERCENTILE(
    numArray: AqlExpression<List<T1>>,
    n: AqlExpression<T2>,
): AqlExpression<Number> =
    AqlFunc.PERCENTILE.numberCall(numArray, n)

/**
 * Return the nth percentile of the values in numArray.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#percentile
 */
@VaultFunctionMarker
fun <T1 : Number?, T2 : Number> PERCENTILE(
    numArray: AqlExpression<List<T1>>,
    n: AqlExpression<T2>,
    method: AqlPercentileMethod,
): AqlExpression<Number> =
    AqlFunc.PERCENTILE.numberCall(numArray, n, method.method.aql)

/**
 * Returns pi.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#pi
 */
@VaultFunctionMarker
fun PI(): AqlExpression<Number> =
    AqlFunc.PI.numberCall()

/**
 * Return the base to the exponent exp.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#pow
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> POW(
    base: AqlExpression<T1>,
    exp: AqlExpression<T2>,
): AqlExpression<Number?> =
    AqlFunc.POW.nullableNumberCall(base, exp)

/**
 * Return the angle converted from degrees to radians.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#radians
 */
@VaultFunctionMarker
fun <T : Number> RADIANS(
    deg: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.RADIANS.numberCall(deg)

/**
 * Return a pseudo-random number between 0 and 1.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#rand
 */
@VaultFunctionMarker
fun RAND(): AqlExpression<Number> =
    AqlFunc.RAND.numberCall()

/**
 * Return an array of numbers in the specified range, optionally with increments other than 1.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#range
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number> RANGE(
    start: AqlExpression<T1>,
    stop: AqlExpression<T2>,
): AqlExpression<List<Number>> =
    AqlFunc.RANGE.arrayCall(type = kListType<Number>(), start, stop)

/**
 * Return an array of numbers in the specified range, optionally with increments other than 1.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#range
 */
@VaultFunctionMarker
fun <T1 : Number, T2 : Number, T3 : Number> RANGE(
    start: AqlExpression<T1>,
    stop: AqlExpression<T2>,
    step: AqlExpression<T3>,
): AqlExpression<List<Number>?> =
    AqlFunc.RANGE.nullableArrayCall(type = kListType<Number>().nullable, start, stop, step)

/**
 * Return the integer closest to value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/Numeric.html#round
 */
@VaultFunctionMarker
fun <T : Number> ROUND(
    value: AqlExpression<T>,
): AqlExpression<Number> =
    AqlFunc.ROUND.numberCall(value)

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
