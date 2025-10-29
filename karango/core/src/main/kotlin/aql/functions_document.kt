@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.karango.vault.KarangoRepository
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.common.reflection.unList
import de.peekandpoke.ultra.vault.ensureKey
import de.peekandpoke.ultra.vault.lang.VaultFunctionMarker

@VaultFunctionMarker val DOCUMENT = aqlFunc<Any?>("DOCUMENT")

/**
 * Get a single document by its full id.
 */
@VaultFunctionMarker
inline fun <reified T> DOCUMENT(id: String): AqlExpression<T> =
    DOCUMENT.call(type = kType(), id.aql("id"))

/**
 * Get a single document by its full id and deserialize it to the given cls
 */
@VaultFunctionMarker
fun <T : Any> DOCUMENT(cls: Class<T>, id: String): AqlExpression<T> =
    DOCUMENT.call(type = cls.kType(), id.aql("id"))

/**
 * Get a single document from the given collection by its key.
 */
@VaultFunctionMarker
fun <T : Any> DOCUMENT(collection: KarangoRepository<T>, id: String): AqlExpression<T> =
    DOCUMENT.call(
        collection.getType().unList,
        "${collection.name}/${id.ensureKey}".aql("id")
    )

/**
 * Get a list of documents by their IDs
 */
@VaultFunctionMarker
inline fun <reified T> DOCUMENT(vararg ids: String): AqlExpression<List<T>> =
    DOCUMENT(ids.toList())

/**
 * Get a list of documents by their IDs
 */
@VaultFunctionMarker
inline fun <reified T> DOCUMENT(ids: List<String>): AqlExpression<List<T>> =
    DOCUMENT.call(kType(), ids.aql("ids"))

/**
 * Get a list of documents from the given collection by their keys.
 */
@VaultFunctionMarker
inline fun <reified T : Any> DOCUMENT(collection: KarangoRepository<T>, vararg ids: String) =
    DOCUMENT(collection, ids.toList())

/**
 * Get a list of documents from the given collection by their keys.
 */
@VaultFunctionMarker
fun <T : Any> DOCUMENT(collection: KarangoRepository<T>, ids: List<String>) =
    DOCUMENT(type = collection.getType(), collection.name, ids)

/**
 * Get a list of documents of the given type from the given collection by their keys.
 */
@VaultFunctionMarker
fun <T> DOCUMENT(type: TypeRef<List<T>>, collection: String, ids: List<String>): AqlExpression<List<T>> =
    DOCUMENT.call(type = type, ids.map { "$collection/${it.ensureKey}" }.aql("ids"))
