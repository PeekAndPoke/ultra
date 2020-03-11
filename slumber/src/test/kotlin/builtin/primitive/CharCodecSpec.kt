package de.peekandpoke.ultra.slumber.builtin.primitive

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

class CharAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Char, nonNullSamples = nonNull, nullableSamples = nullable
)

class CharSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Char, nonNullSamples = nonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly to Char and to Char?
 */
private val nonNull = listOf(
    row('a', 'a'),
    row('b', 'b'),
    row("true", 't'),
    row("FALSE", 'F'),
    row("stuff", 's')
)

/**
 * Samples that map to null. Converting to Char must throw. Converting to Char? must not throw.
 */
private val nullable = listOf(
    null,
    false,
    true,
    "",
    emptyList<Any>(),
    emptyMap<Any, Any>()
)
