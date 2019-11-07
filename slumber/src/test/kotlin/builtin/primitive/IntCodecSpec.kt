package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class IntAwakerSpec : AwakerSpecHelper(
    cls = Int::class, nonNullSamples = nonNull, nullableSamples = nullable
)

class IntSlumberSpec : SlumbererSpecHelper(
    cls = Int::class, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Int and to Int?
 */
private val nonNull = listOf(
    row(0, 0),
    row(0.toByte(), 0),
    row(0.toShort(), 0),
    row(0.toLong(), 0),
    row(0.0, 0),
    row(0.0f, 0),
    row(Int.MAX_VALUE, Int.MAX_VALUE),
    row(Int.MIN_VALUE, Int.MIN_VALUE),

    row("0", 0),
    row(0.0.toString(), 0),
    row(Int.MAX_VALUE.toString(), Int.MAX_VALUE),
    row(Int.MIN_VALUE.toString(), Int.MIN_VALUE)
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
