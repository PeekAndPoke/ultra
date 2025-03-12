package de.peekandpoke.karango

import com.arangodb.ArangoDBException
import de.peekandpoke.ultra.vault.TypedQuery

open class KarangoException(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)

class KarangoQueryException(
    val query: TypedQuery<*>,
    message: String,
    cause: ArangoDBException,
) : KarangoException(message, cause = cause)
