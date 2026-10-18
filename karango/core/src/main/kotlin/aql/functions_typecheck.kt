@file:Suppress("FunctionName")

package io.peekandpoke.karango.aql

/**
 * Checks whether a value is an array value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_ARRAY = aqlFunc<Boolean>("IS_ARRAY")


fun <T> IS_ARRAY(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_ARRAY.call(expr)

/**
 * Checks whether a value is a boolean value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_BOOL = aqlFunc<Boolean>("IS_BOOL")


fun <T> IS_BOOL(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_BOOL.call(expr)

/**
 * Checks whether a value is a datestring
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_DATESTRING = aqlFunc<Boolean>("IS_DATESTRING")


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
val IS_DOCUMENT = aqlFunc<Boolean>("IS_DOCUMENT")


fun <T> IS_DOCUMENT(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_DOCUMENT.call(expr)

/**
 * Return the data type name of value.
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_KEY = aqlFunc<Boolean>("IS_KEY")


fun <T> IS_KEY(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_KEY.call(expr)

/**
 * Checks whether a value is an array value
 *
 * Alias of IS_ARRAY
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_LIST = aqlFunc<Boolean>("IS_LIST")


fun <T> IS_LIST(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_LIST.call(expr)

/**
 * Checks whether a value is a null value
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-type-cast.html
 */
val IS_NULL = aqlFunc<Boolean>("IS_NULL")


fun <T> IS_NULL(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_NULL.call(expr)


fun <T> IS_NOT_NULL(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_NULL(expr).NOT()

/**
 * Checks whether a value is a number value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_NUMBER = aqlFunc<Boolean>("IS_NUMBER")


fun <T> IS_NUMBER(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_NUMBER.call(expr)

/**
 * Checks whether a value is an object value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_OBJECT = aqlFunc<Boolean>("IS_OBJECT")


fun <T> IS_OBJECT(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_OBJECT.call(expr)

/**
 * Checks whether a value is a string value
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val IS_STRING = aqlFunc<Boolean>("IS_STRING")


fun <T> IS_STRING(expr: AqlExpression<T>): AqlExpression<Boolean> = IS_STRING.call(expr)

/**
 * Return the data type name of value.
 *
 * The data type name can be either "null", "bool", "number", "string", "array" or "object".
 *
 * See https://docs.arangodb.com/current/AQL/Functions/TypeCast.html#type-check-functions
 */
val TYPENAME = aqlFunc<String>("TYPENAME")


fun <T> TYPENAME(expr: AqlExpression<T>): AqlExpression<String> = TYPENAME.call(expr)
