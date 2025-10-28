package de.peekandpoke.karango.vault

import de.peekandpoke.karango.aql.AqlExpression
import de.peekandpoke.karango.aql.AqlIterableExpr
import de.peekandpoke.karango.aql.AqlNameExpr
import de.peekandpoke.karango.aql.AqlPathExpr
import de.peekandpoke.karango.aql.AqlPrinter
import de.peekandpoke.karango.aql.property
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Repository

/** Base interface for all karango repositories */
interface KarangoRepository<T : Any> : Repository<T>, AqlExpression<List<T>> {
    /** Helper for accessing the repos this pointer */
    override val repo: KarangoRepository<T> get() = this

    /** Exposes the stored type as an expression */
    val repoExpr: AqlIterableExpr<T> get() = AqlIterableExpr("repo", this)

    override fun print(p: AqlPrinter) {
        p.append(
            AqlNameExpr(name = name, type = TypeRef.String)
        )
    }
}

/** Refers to the _id of a stored document */
inline val <T> AqlExpression<T>._id: AqlPathExpr<String>
    inline get() = property("_id")

/** Refers to the _key of a stored document */
inline val <T> AqlExpression<T>._key: AqlPathExpr<String>
    inline get() = property("_key")

/** Refers to the _rev of a stored document */
inline val <T> AqlExpression<T>._rev: AqlPathExpr<String>
    inline get() = property("_rev")
