package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class ShortAwakerSpec : AwakerSpecHelper(
    cls = Short::class, nonNullSamples = nonNull, nullableSamples = nullable
)

class ShortSlumberSpec : SlumbererSpecHelper(
    cls = Short::class, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Short and to Short?
 */
private val nonNull = listOf(
    row(0, 0.toShort()),
    row(0.toByte(), 0.toShort()),
    row(0.toShort(), 0.toShort()),
    row(0.toLong(), 0.toShort()),
    row(0.0, 0.toShort()),
    row(0.0f, 0.toShort()),
    row(Short.MAX_VALUE, Short.MAX_VALUE),
    row(Short.MIN_VALUE, Short.MIN_VALUE),

    row("0", 0.toShort()),
    row(0.0.toString(), 0.toShort()),
    row(Short.MAX_VALUE.toString(), Short.MAX_VALUE),
    row(Short.MIN_VALUE.toString(), Short.MIN_VALUE)
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
