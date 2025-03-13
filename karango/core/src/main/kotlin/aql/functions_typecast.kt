@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.VaultTypeConversionMarker

// //  TO_BOOL  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tobool
 */
@VaultTypeConversionMarker
fun TO_BOOL(expr: Expression<*>) = AqlFunc.TO_BOOL.boolCall(expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tobool
 */
@VaultTypeConversionMarker
val <T> Expression<T>.TO_BOOL
    get() = TO_BOOL(this)

// //  TO_NUMBER  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
fun TO_NUMBER(expr: Expression<*>) = AqlFunc.TO_NUMBER.numberCall(expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
val <T> Expression<T>.TO_NUMBER
    get() = TO_NUMBER(this)

// //  TO_STRING  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
fun TO_STRING(expr: Expression<*>) = AqlFunc.TO_STRING.stringCall(expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tostring
 */
@VaultTypeConversionMarker
val <T> Expression<T>.TO_STRING
    get() = TO_STRING(this)

// //  TO_ARRAY  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Guard, to NOT wrap a list again
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@JvmName("TO_ARRAY_L")
@VaultTypeConversionMarker
fun <T> TO_ARRAY(expr: Expression<List<T>>): Expression<List<T>> = AqlFunc.TO_ARRAY.arrayCall(expr.getType(), expr)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@JvmName("TO_ARRAY_E")
@VaultTypeConversionMarker
fun <T> TO_ARRAY(expr: Expression<T>): Expression<List<T>> = AqlFunc.TO_ARRAY.arrayCall(expr.getType().list, expr)

/**
 * Guard, to NOT wrap a list again
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@VaultTypeConversionMarker
val <T> Expression<List<T>>.TO_ARRAY
    @JvmName("TO_ARRAY_LV") get() = TO_ARRAY(this)

/**
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#toarray
 */
@VaultTypeConversionMarker
val <T> Expression<T>.TO_ARRAY: Expression<List<T>>
    @JvmName("TO_ARRAY_V") get() = TO_ARRAY(this)

// //  TO_LIST  ////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@JvmName("TO_LIST_L")
@VaultTypeConversionMarker
fun <T> TO_LIST(expr: Expression<List<T>>): Expression<List<T>> = TO_ARRAY(expr)

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@JvmName("TO_LIST_E")
@VaultTypeConversionMarker
fun <T> TO_LIST(expr: Expression<T>): Expression<List<T>> = TO_ARRAY(expr)

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@VaultTypeConversionMarker
val <T> Expression<List<T>>.TO_LIST
    @JvmName("TO_LIST_LV") get() = TO_ARRAY(this)

/**
 * Alias of TO_ARRAY
 *
 * https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#tolist
 */
@VaultTypeConversionMarker
val <T> Expression<T>.TO_LIST: Expression<List<T>>
    @JvmName("TO_LIST_V") get() = this.TO_ARRAY
