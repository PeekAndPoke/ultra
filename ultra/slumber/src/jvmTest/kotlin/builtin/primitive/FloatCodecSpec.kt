package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class FloatAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Float, nonNullSamples = nonNull, nullableSamples = nullable
)

class FloatSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Float, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Float and to Float?
 */
private val nonNull = listOf(
    tuple(0, 0.toFloat()),
    tuple(0.toByte(), 0.toFloat()),
    tuple(0.toShort(), 0.toFloat()),
    tuple(0.toLong(), 0.toFloat()),
    tuple(0.0, 0.toFloat()),
    tuple(0.0f, 0.toFloat()),
    tuple(Float.MAX_VALUE, Float.MAX_VALUE),
    tuple(Float.MIN_VALUE, Float.MIN_VALUE),

    tuple("0", 0.toFloat()),
    tuple(0.0.toString(), 0.toFloat()),
    tuple(Float.MAX_VALUE.toString(), Float.MAX_VALUE),
    tuple(Float.MIN_VALUE.toString(), Float.MIN_VALUE)
)

/**
 * Samples that map to null. Converting to Float must throw. Converting to Float? must not throw.
 */
private val nullable = listOf(
    null,
    false,
    true,
    'a',
    "",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
