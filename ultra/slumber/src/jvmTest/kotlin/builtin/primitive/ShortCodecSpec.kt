package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class ShortAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Short, nonNullSamples = nonNull, nullableSamples = nullable
)

class ShortSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Short, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Short and to Short?
 */
private val nonNull = listOf(
    tuple(0, 0.toShort()),
    tuple(0.toByte(), 0.toShort()),
    tuple(0.toShort(), 0.toShort()),
    tuple(0.toLong(), 0.toShort()),
    tuple(0.0, 0.toShort()),
    tuple(0.0f, 0.toShort()),
    tuple(Short.MAX_VALUE, Short.MAX_VALUE),
    tuple(Short.MIN_VALUE, Short.MIN_VALUE),

    tuple("0", 0.toShort()),
    tuple(0.0.toString(), 0.toShort()),
    tuple(Short.MAX_VALUE.toString(), Short.MAX_VALUE),
    tuple(Short.MIN_VALUE.toString(), Short.MIN_VALUE)
)

/**
 * Samples that map to null. Converting to Short must throw. Converting to Short? must not throw.
 */
private val nullable = listOf(
    Short.MAX_VALUE + 1L,
    Short.MIN_VALUE - 1L,
    null,
    false,
    true,
    'a',
    "",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
