package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class ByteAwakerSpec : AwakerSpecHelper(
    cls = Byte::class, nonNullSamples = nonNull, nullableSamples = nullable
)

class ByteSlumberSpec : SlumbererSpecHelper(
    cls = Byte::class, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Byte and to Byte?
 */
private val nonNull = listOf(
    row(0, 0.toByte()),
    row(0.toByte(), 0.toByte()),
    row(0.toShort(), 0.toByte()),
    row(0.toLong(), 0.toByte()),
    row(0.0, 0.toByte()),
    row(0.0f, 0.toByte()),
    row(Byte.MAX_VALUE, Byte.MAX_VALUE),
    row(Byte.MIN_VALUE, Byte.MIN_VALUE),

    row("0", 0.toByte()),
    row(0.0.toString(), 0.toByte()),
    row(Byte.MAX_VALUE.toString(), Byte.MAX_VALUE),
    row(Byte.MIN_VALUE.toString(), Byte.MIN_VALUE)
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
