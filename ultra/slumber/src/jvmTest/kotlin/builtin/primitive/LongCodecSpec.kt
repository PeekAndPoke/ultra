package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class LongAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Long, nonNullSamples = nonNull, nullableSamples = nullable
)

class LongSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Long, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Long and to Long?
 */
private val nonNull = listOf(
    tuple(0, 0.toLong()),
    tuple(0.toByte(), 0.toLong()),
    tuple(0.toShort(), 0.toLong()),
    tuple(0.toLong(), 0.toLong()),
    tuple(0.0, 0.toLong()),
    tuple(0.0f, 0.toLong()),
    tuple(Long.MAX_VALUE, Long.MAX_VALUE),
    tuple(Long.MIN_VALUE, Long.MIN_VALUE),

    tuple("0", 0.toLong()),
    tuple(0.0.toString(), 0.toLong()),
    tuple(Long.MAX_VALUE.toString(), Long.MAX_VALUE),
    tuple(Long.MIN_VALUE.toString(), Long.MIN_VALUE)
)

/**
 * Samples that map to null. Converting to Long must throw. Converting to Long? must not throw.
 */
private val nullable = listOf(
    Long.MAX_VALUE.toString() + "0",
    Long.MIN_VALUE.toString() + "0",
    null,
    false,
    true,
    'a',
    "",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
