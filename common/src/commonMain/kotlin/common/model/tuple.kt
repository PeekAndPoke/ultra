package de.peekandpoke.ultra.common.model

/**
 * Creates a tuple containing one element.
 *
 * @param e1 The first element of the tuple.
 *
 * @return A tuple containing the specified element.
 */
fun <E1> tuple(
    e1: E1,
) = Tuple1(e1)

/**
 * Creates a tuple containing two elements.
 *
 * @param e1 The first element of the tuple.
 * @param e2 The second element of the tuple.
 *
 * @return A tuple containing the specified elements.
 */
fun <E1, E2> tuple(
    e1: E1,
    e2: E2,
) = Tuple2(e1, e2)

/**
 * Creates a tuple with three elements of different types.
 *
 * @param e1 the first element of the tuple
 * @param e2 the second element of the tuple
 * @param e3 the third element of the tuple
 *
 * @return a new instance of Tuple3 with the specified elements
 */
fun <E1, E2, E3> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
) = Tuple3(e1, e2, e3)

/**
 * Creates a new tuple with the given elements.
 *
 * @param e1 The first element of the tuple.
 * @param e2 The second element of the tuple.
 * @param e3 The third element of the tuple.
 * @param e4 The fourth element of the tuple.
 *
 * @return A new tuple containing the given elements.
 */
fun <E1, E2, E3, E4> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
) = Tuple4(e1, e2, e3, e4)

/**
 * Creates a tuple with five elements.
 *
 * @param e1 The value of the first element.
 * @param e2 The value of the second element.
 * @param e3 The value of the third element.
 * @param e4 The value of the fourth element.
 * @param e5 The value of the fifth element.
 *
 * @return A new tuple with the specified elements.
 */
fun <E1, E2, E3, E4, E5> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
) = Tuple5(e1, e2, e3, e4, e5)

/**
 * Creates a tuple of six elements.
 *
 * @param e1 the first element of the tuple
 * @param e2 the second element of the tuple
 * @param e3 the third element of the tuple
 * @param e4 the fourth element of the tuple
 * @param e5 the fifth element of the tuple
 * @param e6 the sixth element of the tuple
 *
 * @return a tuple containing the specified elements
 */
fun <E1, E2, E3, E4, E5, E6> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
) = Tuple6(e1, e2, e3, e4, e5, e6)

/**
 * Creates a 7-element tuple with the specified values.
 *
 * @param e1 The value of the first element.
 * @param e2 The value of the second element.
 * @param e3 The value of the third element.
 * @param e4 The value of the fourth element.
 * @param e5 The value of the fifth element.
 * @param e6 The value of the sixth element.
 * @param e7 The value of the seventh element.
 *
 * @return A new 7-element tuple with the given values.
 */
fun <E1, E2, E3, E4, E5, E6, E7> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
    e7: E7,
) = Tuple7(e1, e2, e3, e4, e5, e6, e7)

/**
 * Creates a tuple with eight elements of the given types.
 *
 * @param e1 the first element of the tuple
 * @param e2 the second element of the tuple
 * @param e3 the third element of the tuple
 * @param e4 the fourth element of the tuple
 * @param e5 the fifth element of the tuple
 * @param e6 the sixth element of the tuple
 * @param e7 the seventh element of the tuple
 * @param e8 the eighth element of the tuple
 *
 * @return the tuple containing the specified elements
 */
fun <E1, E2, E3, E4, E5, E6, E7, E8> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
    e7: E7,
    e8: E8,
) = Tuple8(e1, e2, e3, e4, e5, e6, e7, e8)

/**
 * Creates a new tuple with nine elements.
 *
 * @param e1 The first element of the tuple.
 * @param e2 The second element of the tuple.
 * @param e3 The third element of the tuple.
 * @param e4 The fourth element of the tuple.
 * @param e5 The fifth element of the tuple.
 * @param e6 The sixth element of the tuple.
 * @param e7 The seventh element of the tuple.
 * @param e8 The eighth element of the tuple.
 * @param e9 The ninth element of the tuple.
 *
 * @return A new tuple with the specified elements.
 */
fun <E1, E2, E3, E4, E5, E6, E7, E8, E9> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
    e7: E7,
    e8: E8,
    e9: E9,
) = Tuple9(e1, e2, e3, e4, e5, e6, e7, e8, e9)

/**
 * Creates a tuple with 10 elements.
 *
 * @param e1 The first element.
 * @param e2 The second element.
 * @param e3 The third element.
 * @param e4 The fourth element.
 * @param e5 The fifth element.
 * @param e6 The sixth element.
 * @param e7 The seventh element.
 * @param e8 The eighth element.
 * @param e9 The ninth element.
 * @param e10 The tenth element.
 *
 * @return A tuple containing the specified elements.
 */
fun <E1, E2, E3, E4, E5, E6, E7, E8, E9, E10> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
    e7: E7,
    e8: E8,
    e9: E9,
    e10: E10,
) = Tuple10(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10)

/**
 * Represents a tuple containing one element.
 *
 * @param E1 The type of the element.
 */
data class Tuple1<E1>(
    val e1: E1,
) {
    /**
     * Returns a list containing the element stored in the calling `Tuple1` instance.
     *
     * @return A lazy evaluated list containing the element.
     */
    val asList by lazy { listOf(e1) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple2<E1, X> = tuple(e1, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple2<E1, X> = this + x
}

/**
 * Represents a tuple containing two elements.
 */
data class Tuple2<E1, E2>(
    val e1: E1,
    val e2: E2,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple3<E1, E2, X> = tuple(e1, e2, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple3<E1, E2, X> = this + x
}

/**
 * Represents a tuple containing three elements.
 */
data class Tuple3<E1, E2, E3>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple4<E1, E2, E3, X> = tuple(e1, e2, e3, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple4<E1, E2, E3, X> = this + x
}

/**
 * Represents a tuple containing four elements.
 */
data class Tuple4<E1, E2, E3, E4>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple5<E1, E2, E3, E4, X> = tuple(e1, e2, e3, e4, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple5<E1, E2, E3, E4, X> = this + x
}

/**
 * Represents a tuple containing five elements.
 */
data class Tuple5<E1, E2, E3, E4, E5>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4, e5) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple6<E1, E2, E3, E4, E5, X> = tuple(e1, e2, e3, e4, e5, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple6<E1, E2, E3, E4, E5, X> = this + x
}

/**
 * Represents a tuple containing six elements.
 */
data class Tuple6<E1, E2, E3, E4, E5, E6>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4, e5, e6) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple7<E1, E2, E3, E4, E5, E6, X> = tuple(e1, e2, e3, e4, e5, e6, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple7<E1, E2, E3, E4, E5, E6, X> = this + x
}

/**
 * Represents a tuple containing seven elements.
 */
data class Tuple7<E1, E2, E3, E4, E5, E6, E7>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
    val e7: E7,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4, e5, e6, e7) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple8<E1, E2, E3, E4, E5, E6, E7, X> = tuple(e1, e2, e3, e4, e5, e6, e7, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple8<E1, E2, E3, E4, E5, E6, E7, X> = this + x
}

/**
 * Represents a tuple containing eight elements.
 */
data class Tuple8<E1, E2, E3, E4, E5, E6, E7, E8>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
    val e7: E7,
    val e8: E8,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4, e5, e6, e7, e8) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple9<E1, E2, E3, E4, E5, E6, E7, E8, X> = tuple(e1, e2, e3, e4, e5, e6, e7, e8, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple9<E1, E2, E3, E4, E5, E6, E7, E8, X> = this + x
}

/**
 * Represents a tuple containing nine elements.
 */
data class Tuple9<E1, E2, E3, E4, E5, E6, E7, E8, E9>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
    val e7: E7,
    val e8: E8,
    val e9: E9,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4, e5, e6, e7, e8, e9) }

    /**
     * Adds an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be added to the tuple.
     *
     * @return A new tuple containing the original element and the additional element.
     */
    operator fun <X> plus(x: X): Tuple10<E1, E2, E3, E4, E5, E6, E7, E8, E9, X> =
        tuple(e1, e2, e3, e4, e5, e6, e7, e8, e9, x)

    /**
     * Appends an element to the current tuple, creating a new tuple with an additional element.
     *
     * @param x The element to be appended to the tuple.
     *
     * @return A new tuple containing the original elements and the additional element.
     */
    fun <X> append(x: X): Tuple10<E1, E2, E3, E4, E5, E6, E7, E8, E9, X> = this + x
}

/**
 * Represents a tuple containing ten elements.
 */
data class Tuple10<E1, E2, E3, E4, E5, E6, E7, E8, E9, E10>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
    val e7: E7,
    val e8: E8,
    val e9: E9,
    val e10: E10,
) {
    /**
     * Lazily converts the elements of this tuple to a list.
     *
     * The resulting list will contain the elements in the same order as they appear in the tuple.
     */
    val asList by lazy { listOf(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10) }
}
