package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotest.data.row

class DoubleAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Double, nonNullSamples = nonNull, nullableSamples = nullable
)

class DoubleSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Double, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Double and to Double?
 */
private val nonNull = listOf(
    row(0, 0.toDouble()),
    row(0.toByte(), 0.toDouble()),
    row(0.toShort(), 0.toDouble()),
    row(0.toLong(), 0.toDouble()),
    row(0.0, 0.toDouble()),
    row(0.0f, 0.toDouble()),
    row(Double.MAX_VALUE, Double.MAX_VALUE),
    row(Double.MIN_VALUE, Double.MIN_VALUE),

    row("0", 0.toDouble()),
    row(0.0.toString(), 0.toDouble()),
    row(Double.MAX_VALUE.toString(), Double.MAX_VALUE),
    row(Double.MIN_VALUE.toString(), Double.MIN_VALUE)
)

/**
 * Samples that map to null. Converting to Double must throw. Converting to Double? must not throw.
 */
private val nullable = listOf(
    null,
    false,
    true,
    'a',
    "",
    "a1",
    "1a",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
