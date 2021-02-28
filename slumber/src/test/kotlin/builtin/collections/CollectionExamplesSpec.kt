package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper
import io.kotest.data.Row2
import io.kotest.data.row

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

/**
 * Spec for awaking lists of ints
 */
class IntListAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Int.list, nonNullSamples = intListNonNull, nullableSamples = nullable
)

/**
 * Spec for slumbering lists of ints
 */
class IntListSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Int.list, nonNullSamples = intListNonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly.
 */
private val intListNonNull = listOf(
    row(emptyList<Int>(), emptyList()),
    row(emptyArray<Int>(), emptyList()),
    row(listOf(1, 2), listOf(1, 2)),
    row(listOf(1.1, 2.2), listOf(1, 2)),
    row(listOf("10", 20.toShort()), listOf(10, 20))
)

/**
 * Spec for awaking lists of ints
 */
class MutableIntListAwakerSpec : AwakerSpecHelper(
    type = TypeRef.Int.wrapWith<MutableList<*>>(),
    nonNullSamples = mutableIntListNonNull,
    nullableSamples = nullable
)

/**
 * Spec for slumbering lists of ints
 */
class MutableIntListSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.Int.wrapWith<MutableList<*>>(),
    nonNullSamples = mutableIntListNonNull,
    nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly.
 */
private val mutableIntListNonNull: List<Row2<Any, List<Int>>> = listOf(
    row(emptyList<Int>(), emptyList()),
    row(emptyArray<Int>(), emptyList()),
    row(listOf(1, 2), mutableListOf(1, 2)),
    row(listOf(1.1, 2.2), mutableListOf(1, 2)),
    row(listOf("10", 20.toShort()), mutableListOf(10, 20))
)

/**
 * Spec for awaking lists of lists of strings
 */
class ListOfStringListAwakerSpec : AwakerSpecHelper(
    type = TypeRef.String.list.list, nonNullSamples = listOfStringListNonNull, nullableSamples = nullable
)

/**
 * Spec for slumbering lists of lists of strings
 */
class ListOfStringListSlumberSpec : SlumbererSpecHelper(
    type = TypeRef.String.list.list, nonNullSamples = listOfStringListNonNull, nullableSamples = nullable
)

/**
 * Samples that map properly. These must be converted correctly.
 */
private val listOfStringListNonNull = listOf(
    row(
        emptyList<Any>(),
        emptyList<List<String>>()
    ),
    row(
        emptyArray<Any>(),
        emptyList<List<String>>()
    ),
    row(
        listOf(emptyList<Any>()),
        listOf(emptyList<Any>())
    ),
    row(
        listOf(listOf(1, 2), listOf(3.3, 4.4)),
        listOf(listOf("1", "2"), listOf("3.3", "4.4"))
    ),
    row(
        listOf(listOf("", "a"), listOf()),
        listOf(listOf("", "a"), listOf())
    )
)
