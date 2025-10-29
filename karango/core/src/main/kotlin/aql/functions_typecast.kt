@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultTypeConversionMarker

// //  TO_BOOL  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tobool
 */
@VaultTypeConversionMarker val TO_BOOL = aqlFunc<Boolean>("TO_BOOL")

@VaultTypeConversionMarker
fun TO_BOOL(expr: AqlExpression<*>): AqlExpression<Boolean> = TO_BOOL.call(expr)

@VaultTypeConversionMarker
val <T> AqlExpression<T>.TO_BOOL: AqlExpression<Boolean> get() = TO_BOOL(this)

// //  TO_NUMBER  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker val TO_NUMBER = aqlFunc<Number>("TO_NUMBER")

@VaultTypeConversionMarker
fun TO_NUMBER(expr: AqlExpression<*>): AqlExpression<Number> = TO_NUMBER.call(expr)

@VaultTypeConversionMarker
val <T> AqlExpression<T>.TO_NUMBER: AqlExpression<Number> get() = TO_NUMBER(this)

// //  TO_STRING  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker val TO_STRING = aqlFunc<String>("TO_STRING")

@VaultTypeConversionMarker
fun TO_STRING(expr: AqlExpression<*>): AqlExpression<String> = TO_STRING.call(expr)

@VaultTypeConversionMarker
val <T> AqlExpression<T>.TO_STRING: AqlExpression<String> get() = TO_STRING(this)

// //  TO_ARRAY  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Guard, to NOT wrap a list again
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@VaultTypeConversionMarker val TO_ARRAY = aqlFunc<List<Any?>>("TO_ARRAY")

@JvmName("TO_ARRAY_L")
@VaultTypeConversionMarker
inline fun <reified T> TO_ARRAY(expr: AqlExpression<out Collection<T>>): AqlExpression<List<T>> =
    TO_ARRAY.call(type = kType(), expr)

@JvmName("TO_ARRAY_E")
@VaultTypeConversionMarker
inline fun <reified T> TO_ARRAY(expr: AqlExpression<T>): AqlExpression<List<T>> =
    TO_ARRAY.call(type = kType(), expr)

@VaultTypeConversionMarker
inline val <reified T> AqlExpression<out Collection<T>>.TO_ARRAY: AqlExpression<List<T>>
    @JvmName("TO_ARRAY_LV") get() = TO_ARRAY(this)

@VaultTypeConversionMarker
inline val <reified T> AqlExpression<T>.TO_ARRAY: AqlExpression<List<T>>
    @JvmName("TO_ARRAY_V") get() = TO_ARRAY(this)

// //  TO_LIST  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@VaultTypeConversionMarker val TO_LIST = aqlFunc<List<Any?>>("TO_LIST")

@JvmName("TO_LIST_L")
@VaultTypeConversionMarker
inline fun <reified T> TO_LIST(expr: AqlExpression<out Collection<T>>): AqlExpression<List<T>> =
    TO_LIST.call(type = kType(), expr)

@JvmName("TO_LIST_E")
@VaultTypeConversionMarker
inline fun <reified T> TO_LIST(expr: AqlExpression<T>): AqlExpression<List<T>> =
    TO_LIST.call(type = kType(), expr)

@VaultTypeConversionMarker
inline val <reified T> AqlExpression<out Collection<T>>.TO_LIST: AqlExpression<List<T>>
    @JvmName("TO_LIST_LV") get() = TO_LIST(this)

@VaultTypeConversionMarker
inline val <reified T> AqlExpression<T>.TO_LIST: AqlExpression<List<T>>
    @JvmName("TO_LIST_V") get() = TO_LIST(this)
