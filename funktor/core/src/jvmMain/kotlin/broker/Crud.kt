package de.peekandpoke.ktorfx.core.broker

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Stored

/**
 * Creates crud routes for the given type [T] and the given [uri]
 */
inline fun <reified T : Any> Routes.crud(uri: String) = Crud<T>(uri, this, kType())

/**
 * Helper for creating crud routes
 */
class Crud<T : Any>(val uri: String, routes: Routes, typeRef: TypeRef<T>) {

    data class List(val search: String = "")

    data class Edit<T : Any>(val item: Stored<T>) {
        override fun toString() = item._id
    }

    val list = routes.route<List>(uri)
    fun list(search: String = "") = list(List(search = search))

    val create = routes.route("$uri/create")

    val edit = routes.route(typeRef.wrapWith<Edit<T>>(), "$uri/{item}/edit")
    fun edit(item: Stored<T>) = edit(Edit(item))

    val delete = routes.route(typeRef.wrapWith<Edit<T>>(), "$uri/{item}/delete/{csrf}")
    fun delete(item: Stored<T>) = delete(Edit(item))
}
