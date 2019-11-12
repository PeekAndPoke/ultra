package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotlintest.tables.row

/**
 * Samples that map to null. Converting to Byte must throw. Converting must not throw.
 */
private val nullable = listOf(
    0.toByte(),
    0,
    null,
    false,
    true,
    'a',
    "",
    emptyMap<Any, Any>()
)

class IntListAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Int.list, nonNullSamples = intListNonNull, nullableSamples = nullable
)

class IntListSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Int.list, nonNullSamples = intListNonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly.
 */
private val intListNonNull = listOf(
    row(emptyList<Int>(), emptyList()),
    row(emptyArray<Int>(), emptyList<Int>()),
    row(listOf(1, 2), listOf(1, 2))
)

