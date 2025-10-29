@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Checks whether a value is an array value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_ARRAY = aqlFunc<Boolean>("IS_ARRAY")

@VaultFunctionMarker
fun <T> IS_ARRAY(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_ARRAY.call(expr)

/**
 * Checks whether a value is a boolean value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_BOOL = aqlFunc<Boolean>("IS_BOOL")

@VaultFunctionMarker
fun <T> IS_BOOL(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_BOOL.call(expr)

/**
 * Checks whether a value is a datestring
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_DATESTRING = aqlFunc<Boolean>("IS_DATESTRING")

@VaultFunctionMarker
fun <T> IS_DATESTRING(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_DATESTRING.call(expr)

/**
 * Checks whether a value is a document
 *
 * All Key-value-objects seem to be recognized as documents.
 *
 * Alias for IS_OBJECT
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_DOCUMENT = aqlFunc<Boolean>("IS_DOCUMENT")

@VaultFunctionMarker
fun <T> IS_DOCUMENT(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_DOCUMENT.call(expr)

/**
 * Return the data type name of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_KEY = aqlFunc<Boolean>("IS_KEY")

@VaultFunctionMarker
fun <T> IS_KEY(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_KEY.call(expr)

/**
 * Checks whether a value is an array value
 *
 * Alias of IS_ARRAY
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_LIST = aqlFunc<Boolean>("IS_LIST")

@VaultFunctionMarker
fun <T> IS_LIST(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_LIST.call(expr)

/**
 * Checks whether a value is a null value
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-type-cast.html
 */
@VaultFunctionMarker val IS_NULL = aqlFunc<Boolean>("IS_NULL")

@VaultFunctionMarker
fun <T> IS_NULL(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_NULL.call(expr)

@VaultFunctionMarker
fun <T> IS_NOT_NULL(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_NULL(expr).NOT()

/**
 * Checks whether a value is a number value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_NUMBER = aqlFunc<Boolean>("IS_NUMBER")

@VaultFunctionMarker
fun <T> IS_NUMBER(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_NUMBER.call(expr)

/**
 * Checks whether a value is an object value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_OBJECT = aqlFunc<Boolean>("IS_OBJECT")

@VaultFunctionMarker
fun <T> IS_OBJECT(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_OBJECT.call(expr)

/**
 * Checks whether a value is a string value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val IS_STRING = aqlFunc<Boolean>("IS_STRING")

@VaultFunctionMarker
fun <T> IS_STRING(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_STRING.call(expr)

/**
 * Return the data type name of value.
 *
 * The data type name can be either "null", "bool", "number", "string", "array" or "object".
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
@VaultFunctionMarker val TYPENAME = aqlFunc<String>("TYPENAME")

@VaultFunctionMarker
fun <T> TYPENAME(expr: AqlExpression<T>): AqlExpression<String> = TYPENAME.call(expr)
