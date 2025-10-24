package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

class BooleanAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Boolean, nonNullSamples = nonNull, nullableSamples = nullable
)

class BooleanSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Boolean, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Boolean and to Boolean?
 */
private val nonNull = listOf(
    @Suppress("BooleanLiteralArgument")
    tuple(true, true),
    tuple(1, true),
    tuple(2, true),
    tuple(1.1, true),
    tuple("true", true),
    tuple("1", true),
    tuple("2", true),

    @Suppress("BooleanLiteralArgument")
    tuple(false, false),
    tuple(0, false),
    tuple(0.9999, false),
    tuple("0", false),
    tuple("false", false),
    tuple("stuff", false)
)

/**
 * Samples that map to null. Converting to Boolean must throw. Converting to Boolean? must not throw.
 */
private val nullable = listOf(
    null,
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
