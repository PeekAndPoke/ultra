package de.peekandpoke.ultra.slumber.builtin.collections

import de.peekandpoke.ultra.common.model.Tuple2
import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.slumber.builtin.AwakerSpecHelper
import de.peekandpoke.ultra.slumber.builtin.SlumbererSpecHelper

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
    tuple(emptyList<Int>(), emptyList()),
    tuple(emptyArray<Int>(), emptyList()),
    tuple(listOf(1, 2), listOf(1, 2)),
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
private val mutableIntListNonNull: List<Tuple2<Any, List<Int>>> = listOf(
    tuple(emptyList<Int>(), emptyList()),
    tuple(emptyArray<Int>(), emptyList()),
    tuple(listOf(1, 2), mutableListOf(1, 2)),
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
    tuple(
        emptyList<Any>(),
        emptyList<List<String>>()
    ),
    tuple(
        emptyArray<Any>(),
        emptyList<List<String>>()
    ),
    tuple(
        listOf(emptyList<Any>()),
        listOf(emptyList<Any>())
    ),
    tuple(
        listOf(listOf("", "a"), listOf()),
        listOf(listOf("", "a"), listOf())
    )
)
