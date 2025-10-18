package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

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
    tuple(0, 0),
    tuple(0.toByte(), 0.toByte()),
    tuple(0.toShort(), 0.toShort()),
    tuple(0.toLong(), 0.toLong()),
    tuple(0.0, 0.0),
    tuple(0.0f, 0.0f),

    tuple("0", 0.0),
    tuple(0.0.toString(), 0.0)
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
