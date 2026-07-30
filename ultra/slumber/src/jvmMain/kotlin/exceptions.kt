package io.peekandpoke.ultra.slumber

import kotlin.reflect.KType

/**
 * Base exception for all Slumber serialization/deserialization errors.
 *
 * Never thrown directly; only [AwakerException] and [SlumbererException] are raised. [Codec] catches
 * this type to trigger the diagnostic second pass.
 */
// TODO(scan): extends Throwable, not Exception -- `catch (e: Exception)` around a codec call does not
//  catch it (see funktor/cluster/.../BackgroundJobQueued.kt:36). Also has no `cause` parameter, so a
//  reflection failure underneath cannot be wrapped without losing the stack.
open class SlumberException(message: String) : Throwable(message)

/**
 * Thrown when deserialization fails.
 *
 * Raised by [Awaker.Context.reportNullError], i.e. by [NonNullAwaker] when an inner awaker yields null
 * for a non-nullable type. [logs] holds the second-pass diagnostic trail, empty on the first pass.
 */
class AwakerException(
    message: String,
    val logs: List<String>,
    val rootType: KType?,
    val input: Any?,
) : SlumberException(message)

/**
 * Thrown when serialization fails.
 *
 * Raised by [Slumberer.Context.reportNullError], i.e. by [NonNullSlumberer] when an inner slumberer
 * yields null for a non-nullable type.
 */
class SlumbererException(
    message: String,
    val input: Any?,
) : SlumberException(message)
