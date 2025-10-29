@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultTypeConversionMarker

// //  TO_BOOL  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tobool
 */
@VaultTypeConversionMarker
fun TO_BOOL(expr: AqlExpression<*>): AqlExpression<Boolean> =
    AqlFunc.TO_BOOL.boolCall(expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tobool
 */
@VaultTypeConversionMarker
val <T> AqlExpression<T>.TO_BOOL: AqlExpression<Boolean>
    get() = TO_BOOL(this)

// //  TO_NUMBER  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
fun TO_NUMBER(expr: AqlExpression<*>): AqlExpression<Number> =
    AqlFunc.TO_NUMBER.numberCall(expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
val <T> AqlExpression<T>.TO_NUMBER: AqlExpression<Number>
    get() = TO_NUMBER(this)

// //  TO_STRING  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
fun TO_STRING(expr: AqlExpression<*>): AqlExpression<String> =
    AqlFunc.TO_STRING.stringCall(expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
val <T> AqlExpression<T>.TO_STRING: AqlExpression<String>
    get() = TO_STRING(this)

// //  TO_ARRAY  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Guard, to NOT wrap a list again
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@JvmName("TO_ARRAY_L")
@VaultTypeConversionMarker
inline fun <reified T> TO_ARRAY(expr: AqlExpression<out Collection<T>>): AqlExpression<List<T>> =
    AqlFunc.TO_ARRAY.arrayCall(type = kType(), expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@JvmName("TO_ARRAY_E")
@VaultTypeConversionMarker
inline fun <reified T> TO_ARRAY(expr: AqlExpression<T>): AqlExpression<List<T>> =
    AqlFunc.TO_ARRAY.arrayCall(type = kType(), expr)

/**
 * Guard, to NOT wrap a list again
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@VaultTypeConversionMarker
inline val <reified T> AqlExpression<Collection<T>>.TO_ARRAY: AqlExpression<List<T>>
    @JvmName("TO_ARRAY_LV") get() = TO_ARRAY(this)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@VaultTypeConversionMarker
inline val <reified T> AqlExpression<T>.TO_ARRAY: AqlExpression<List<T>>
    @JvmName("TO_ARRAY_V") get() = TO_ARRAY(this)

// //  TO_LIST  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@JvmName("TO_LIST_L")
@VaultTypeConversionMarker
inline fun <reified T> TO_LIST(expr: AqlExpression<out Collection<T>>): AqlExpression<List<T>> =
    AqlFunc.TO_LIST.arrayCall(type = kType(), expr)

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@JvmName("TO_LIST_E")
@VaultTypeConversionMarker
inline fun <reified T> TO_LIST(expr: AqlExpression<T>): AqlExpression<List<T>> =
    AqlFunc.TO_LIST.arrayCall(type = kType(), expr)


/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@VaultTypeConversionMarker
inline val <reified T> AqlExpression<List<T>>.TO_LIST: AqlExpression<List<T>>
    @JvmName("TO_LIST_LV") get() = TO_LIST(this)

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@VaultTypeConversionMarker
inline val <reified T> AqlExpression<T>.TO_LIST: AqlExpression<List<T>>
    @JvmName("TO_LIST_V") get() = TO_LIST(this)
