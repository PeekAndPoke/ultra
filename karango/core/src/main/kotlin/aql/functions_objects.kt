@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

/**
 * Merge the documents document1 to documentN into a single document. If document attribute keys are ambiguous,
 * the merged result will contain the values of the documents contained later in the argument list.
 *
 * See https://www.arangodb.com/docs/stable/aql/functions-document.html
 */
@VaultFunctionMarker
inline fun <reified T> MERGE(
    document1: Expression<out T>,
    document2: Expression<out T>,
): Expression<T> =
    AqlFunc.MERGE.call(kType(), document1, document2)

@VaultFunctionMarker
inline fun <reified T> MERGE(
    document1: Expression<out T>,
    document2: Expression<out T>,
    vararg documentN: Expression<out T>,
): Expression<T> =
    AqlFunc.MERGE.call(kType(), document1, document2, *documentN)

@VaultFunctionMarker
inline fun <reified T> UNSET(
    document: Expression<out T>,
    attributeName1: Expression<String>,
    vararg attributeNameN: Expression<String>,
): Expression<T> =
    AqlFunc.UNSET.call(kType(), document, attributeName1, *attributeNameN)
