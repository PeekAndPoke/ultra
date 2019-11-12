package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class NumberAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Number, nonNullSamples = nonNull, nullableSamples = nullable
)

class NumberSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Number, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Number and to Number?
 */
private val nonNull = listOf(
    row(0, 0),
    row(0.toByte(), 0.toByte()),
    row(0.toShort(), 0.toShort()),
    row(0.toLong(), 0.toLong()),
    row(0.0, 0.0),
    row(0.0f, 0.0f),

    row("0", 0.0),
    row(0.0.toString(), 0.0)
)

/**
 * Samples that map to null. Converting to Number must throw. Converting to Number? must not throw.
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
