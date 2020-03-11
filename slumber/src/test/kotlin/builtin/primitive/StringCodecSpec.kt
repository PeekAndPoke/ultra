package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class StringAwakerSpec : AwakerSpecHelper(
    type = TypeRef.String, nonNullSamples = nonNull, nullableSamples = nullable
)

class StringSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.String, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to String and to String?
 */
private val nonNull = listOf(
    row(0, 0.toString()),
    row(0.toByte(), 0.toString()),
    row(0.toShort(), 0.toString()),
    row(0.toLong(), 0.toString()),
    row(0.0, 0.0.toString()),
    row(0.1, 0.1.toString()),
    row(0.0f, 0.0.toString()),
    row(0.1f, 0.1f.toString()),

    row(true, "true"),
    row(false, "false"),
    row('c', "c"),
    row("", ""),
    row(" ", " "),
    row("abc", "abc")
)

/**
 * Samples that map to null. Converting to String must throw. Converting to String? must not throw.
 */
private val nullable = listOf(
    null,
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
