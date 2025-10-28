package de.peekandpoke.karango

import com.arangodb.ArangoDBException
import de.peekandpoke.karango.vault.AqlTypedQuery

open class KarangoException(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)

class KarangoQueryException(
    val query: AqlTypedQuery<*>,
    message: String,
    cause: ArangoDBException,
) : KarangoException(message, cause = cause)
