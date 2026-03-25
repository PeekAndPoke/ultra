package io.peekandpoke.ultra.slumber

import kotlin.reflect.KType

/** Base exception for all Slumber serialization/deserialization errors. */
open class SlumberException(message: String) : Throwable(message)

/** Thrown when deserialization fails. Contains the path, diagnostic [logs], the [rootType], and the offending [input]. */
class AwakerException(
    message: String,
    val logs: List<String>,
    val rootType: KType?,
    val input: Any?,
) : SlumberException(message)

/** Thrown when serialization fails (e.g. null value for a non-nullable type). Contains the offending [input]. */
class SlumbererException(
    message: String,
    val input: Any?,
) : SlumberException(message)
