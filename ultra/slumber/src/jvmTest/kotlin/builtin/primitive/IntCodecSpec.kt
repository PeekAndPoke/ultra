package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class IntAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Int, nonNullSamples = nonNull, nullableSamples = nullable
)

class IntSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Int, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Int and to Int?
 */
private val nonNull = listOf(
    tuple(0, 0),
    tuple(0.toByte(), 0),
    tuple(0.toShort(), 0),
    tuple(0.toLong(), 0),
    tuple(0.0, 0),
    tuple(0.0f, 0),
    tuple(Int.MAX_VALUE, Int.MAX_VALUE),
    tuple(Int.MIN_VALUE, Int.MIN_VALUE),

    tuple("0", 0),
    tuple(0.0.toString(), 0),
    tuple(Int.MAX_VALUE.toString(), Int.MAX_VALUE),
    tuple(Int.MIN_VALUE.toString(), Int.MIN_VALUE)
)

/**
 * Samples that map to null. Converting to Int must throw. Converting to Int? must not throw.
 */
private val nullable = listOf(
    Int.MAX_VALUE + 1L,
    Int.MIN_VALUE - 1L,
    null,
    false,
    true,
    'a',
    "",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
