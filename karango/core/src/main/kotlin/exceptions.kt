package de.peekandpoke.karango

import com.arangodb.ArangoDBException
import de.peekandpoke.karango.vault.AqlTypedQuery

/** Base exception for all Karango errors. */
open class KarangoException(
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)

/** Thrown when an AQL query fails. Contains the [query] that caused the error. */
class KarangoQueryException(
    val query: AqlTypedQuery<*>,
    message: String,
    cause: ArangoDBException,
) : KarangoException(message, cause = cause)
