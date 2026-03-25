package io.peekandpoke.monko.vault.vault

import io.peekandpoke.monko.lang.MongoExpression
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.vault.TypedQuery

data class MongoTypedQuery<T>(
    override val root: MongoExpression<List<T>>,
    override val query: String,
    override val vars: Map<String, Any?>,
) : TypedQuery<T> {

    companion object {
        fun <T> of(type: TypeRef<T>, query: String = "", vars: Map<String, Any?> = emptyMap()) = MongoTypedQuery(
            root = EmptyExpression(type.list),
            query = query,
            vars = vars,
        )
    }

    @PublishedApi
    internal class EmptyExpression<T>(private val type: TypeRef<T>) : MongoExpression<T> {
        override fun getType(): TypeRef<T> {
            return type
        }

        override fun print(p: MongoPrinter) {
            // noop
        }
    }
}
