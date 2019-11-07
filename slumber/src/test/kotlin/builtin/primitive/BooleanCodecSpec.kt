package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class BooleanAwakerSpec : AwakerSpecHelper(
    cls = Boolean::class, nonNullSamples = nonNull, nullableSamples = nullable
)

class BooleanSlumberSpec : SlumbererSpecHelper(
    cls = Boolean::class, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Boolean and to Boolean?
 */
private val nonNull = listOf(
    @Suppress("BooleanLiteralArgument")
    row(true, true),
    row(1, true),
    row(2, true),
    row(1.1, true),
    row("true", true),
    row("1", true),
    row("2", true),

    @Suppress("BooleanLiteralArgument")
    row(false, false),
    row(0, false),
    row(0.9999, false),
    row("0", false),
    row("false", false),
    row("stuff", false)
)

/**
 * Samples that map to null. Converting to Boolean must throw. Converting to Boolean? must not throw.
 */
private val nullable = listOf(
    null,
    emptyList<Any>(),
    emptyMap<Any, Any>()
)

