package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class StringAwakerSpec : AwakerSpecHelper(
    type = TypeRef.String, nonNullSamples = nonNull, nullableSamples = nullable
)

class StringSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.String, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to String and to String?
 */
private val nonNull = listOf(
    tuple(0, 0.toString()),
    tuple(0.toByte(), 0.toString()),
    tuple(0.toShort(), 0.toString()),
    tuple(0.toLong(), 0.toString()),
    tuple(0.0, 0.0.toString()),
    tuple(0.1, 0.1.toString()),
    tuple(0.0f, 0.0.toString()),
    tuple(0.1f, 0.1f.toString()),

    tuple(true, "true"),
    tuple(false, "false"),
    tuple('c', "c"),
    tuple("", ""),
    tuple(" ", " "),
    tuple("abc", "abc")
)

/**
 * Samples that map to null. Converting to String must throw. Converting to String? must not throw.
 */
private val nullable = listOf(
    null,
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
