@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Merge the documents document1 to documentN into a single document. If document attribute keys are ambiguous,
 * the merged result will contain the values of the documents contained later in the argument list.
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-document.html
 */
@VaultFunctionMarker
inline fun <reified T> MERGE(
    document1: AqlExpression<out T>,
    document2: AqlExpression<out T>,
): AqlExpression<T> =
    AqlFunc.MERGE.call(type = kType(), document1, document2)

@VaultFunctionMarker
inline fun <reified T> MERGE(
    document1: AqlExpression<out T>,
    document2: AqlExpression<out T>,
    vararg documentN: AqlExpression<out T>,
): AqlExpression<T> =
    AqlFunc.MERGE.call(type = kType(), document1, document2, *documentN)

@VaultFunctionMarker
inline fun <reified T> UNSET(
    document: AqlExpression<out T>,
    attributeName1: AqlExpression<String>,
    vararg attributeNameN: AqlExpression<String>,
): AqlExpression<T> =
    AqlFunc.UNSET.call(type = kType(), document, attributeName1, *attributeNameN)
