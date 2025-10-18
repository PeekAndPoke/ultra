package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class ByteAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Byte, nonNullSamples = nonNull, nullableSamples = nullable
)

class ByteSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Byte, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Byte and to Byte?
 */
private val nonNull = listOf(
    tuple(0, 0.toByte()),
    tuple(0.toByte(), 0.toByte()),
    tuple(0.toShort(), 0.toByte()),
    tuple(0.toLong(), 0.toByte()),
    tuple(0.0, 0.toByte()),
    tuple(0.0f, 0.toByte()),
    tuple(Byte.MAX_VALUE, Byte.MAX_VALUE),
    tuple(Byte.MIN_VALUE, Byte.MIN_VALUE),

    tuple("0", 0.toByte()),
    tuple(0.0.toString(), 0.toByte()),
    tuple(Byte.MAX_VALUE.toString(), Byte.MAX_VALUE),
    tuple(Byte.MIN_VALUE.toString(), Byte.MIN_VALUE)
)

/**
 * Samples that map to null. Converting to Byte must throw. Converting to Byte? must not throw.
 */
private val nullable = listOf(
    Byte.MAX_VALUE + 1,
    Byte.MIN_VALUE - 1,
    null,
    false,
    true,
    'a',
    "",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
