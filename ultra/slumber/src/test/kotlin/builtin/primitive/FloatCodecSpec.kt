package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotest.data.row

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
    row(0, 0.toFloat()),
    row(0.toByte(), 0.toFloat()),
    row(0.toShort(), 0.toFloat()),
    row(0.toLong(), 0.toFloat()),
    row(0.0, 0.toFloat()),
    row(0.0f, 0.toFloat()),
    row(Float.MAX_VALUE, Float.MAX_VALUE),
    row(Float.MIN_VALUE, Float.MIN_VALUE),

    row("0", 0.toFloat()),
    row(0.0.toString(), 0.toFloat()),
    row(Float.MAX_VALUE.toString(), Float.MAX_VALUE),
    row(Float.MIN_VALUE.toString(), Float.MIN_VALUE)
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
