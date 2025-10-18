package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

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
    tuple(0, 0.toDouble()),
    tuple(0.toByte(), 0.toDouble()),
    tuple(0.toShort(), 0.toDouble()),
    tuple(0.toLong(), 0.toDouble()),
    tuple(0.0, 0.toDouble()),
    tuple(0.0f, 0.toDouble()),
    tuple(Double.MAX_VALUE, Double.MAX_VALUE),
    tuple(Double.MIN_VALUE, Double.MIN_VALUE),

    tuple("0", 0.toDouble()),
    tuple(0.0.toString(), 0.toDouble()),
    tuple(Double.MAX_VALUE.toString(), Double.MAX_VALUE),
    tuple(Double.MIN_VALUE.toString(), Double.MIN_VALUE)
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
