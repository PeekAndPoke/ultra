@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Checks whether a value is a null value
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-type-cast.html
 */
@VaultFunctionMarker
fun <T> IS_NULL(expr: Expression<T>) = AqlFunc.IS_NULL.boolCall(expr)

/**
 * Checks whether a value is a null value
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-type-cast.html
 */
@VaultFunctionMarker
fun <T> IS_NOT_NULL(expr: Expression<T>) = IS_NULL(expr).NOT()

/**
 * Checks whether a value is a boolean value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_BOOL(expr: Expression<T>) = AqlFunc.IS_BOOL.boolCall(expr)

/**
 * Checks whether a value is a number value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_NUMBER(expr: Expression<T>) = AqlFunc.IS_NUMBER.boolCall(expr)

/**
 * Checks whether a value is a string value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_STRING(expr: Expression<T>) = AqlFunc.IS_STRING.boolCall(expr)

/**
 * Checks whether a value is an array value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_ARRAY(expr: Expression<T>) = AqlFunc.IS_ARRAY.boolCall(expr)

/**
 * Checks whether a value is an array value
 *
 * Alias of IS_ARRAY
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_LIST(expr: Expression<T>) = AqlFunc.IS_LIST.boolCall(expr)

/**
 * Checks whether a value is an object value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_OBJECT(expr: Expression<T>) = AqlFunc.IS_OBJECT.boolCall(expr)

/**
 * Checks whether a value is a document
 *
 * All Key-value-objects seem to be recognized as documents.
 *
 * Alias for IS_OBJECT
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_DOCUMENT(expr: Expression<T>) = AqlFunc.IS_DOCUMENT.boolCall(expr)

/**
 * Checks whether a value is a datestring
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_DATESTRING(expr: Expression<T>) = AqlFunc.IS_DATESTRING.boolCall(expr)

/**
 * Return the data type name of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> IS_KEY(expr: Expression<T>) = AqlFunc.IS_KEY.boolCall(expr)

/**
 * Return the data type name of value.
 *
 * The data type name can be either "null", "bool", "number", "string", "array" or "object".
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker
fun <T> TYPENAME(expr: Expression<T>) = AqlFunc.TYPENAME.stringCall(expr)
