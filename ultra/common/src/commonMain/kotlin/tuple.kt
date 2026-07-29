package io.peekandpoke.ultra.common

/**
 * Creates a [Tuple1] from the given element.
 *
 * The `tuple(...)` overloads cover the arities [Tuple1] to [Tuple10] and spare the call site from
 * naming the arity explicitly.
 */
fun <E1> tuple(
    e1: E1,
) = Tuple1(e1)

/** Creates a [Tuple2] from the given elements. */
fun <E1, E2> tuple(
    e1: E1,
    e2: E2,
) = Tuple2(e1, e2)

/** Creates a [Tuple3] from the given elements. */
fun <E1, E2, E3> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
) = Tuple3(e1, e2, e3)

/** Creates a [Tuple4] from the given elements. */
fun <E1, E2, E3, E4> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
) = Tuple4(e1, e2, e3, e4)

/** Creates a [Tuple5] from the given elements. */
fun <E1, E2, E3, E4, E5> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
) = Tuple5(e1, e2, e3, e4, e5)

/** Creates a [Tuple6] from the given elements. */
fun <E1, E2, E3, E4, E5, E6> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
) = Tuple6(e1, e2, e3, e4, e5, e6)

/** Creates a [Tuple7] from the given elements. */
fun <E1, E2, E3, E4, E5, E6, E7> tuple(
    e1: E1,
    e2: E2,
    e3: E3,
    e4: E4,
    e5: E5,
    e6: E6,
    e7: E7,
) = Tuple7(e1, e2, e3, e4, e5, e6, e7)

/** Creates a [Tuple8] from the given elements. */
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

/** Creates a [Tuple9] from the given elements. */
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

/** Creates a [Tuple10] from the given elements. */
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
 * A tuple of one element.
 *
 * [Tuple1] to [Tuple10] form one family of fixed-size element holders. [plus] and [append] grow a
 * tuple by one element and always append at the end; [Tuple10] is the largest arity and therefore
 * has neither.
 *
 * `asList` is lazy in [LazyThreadSafetyMode.NONE]: tuples are created in bulk and `asList` is read
 * rarely, so the default synchronized mode would allocate a lock per tuple for a value that usually
 * goes untouched. Do not read `asList` of one instance from several threads at once.
 *
 * @param E1 The type of the element.
 */
data class Tuple1<E1>(
    val e1: E1,
) {
    /** The element as a list, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1) }

    /** Returns a [Tuple2] with [x] appended. */
    operator fun <X> plus(x: X): Tuple2<E1, X> = tuple(e1, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple2<E1, X> = this + x
}

/** A tuple of two elements. See [Tuple1] for the conventions shared by the whole family. */
data class Tuple2<E1, E2>(
    val e1: E1,
    val e2: E2,
) {
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2) }

    /** Returns a [Tuple3] with [x] appended. */
    operator fun <X> plus(x: X): Tuple3<E1, E2, X> = tuple(e1, e2, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple3<E1, E2, X> = this + x
}

/** A tuple of three elements. See [Tuple1] for the conventions shared by the whole family. */
data class Tuple3<E1, E2, E3>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
) {
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3) }

    /** Returns a [Tuple4] with [x] appended. */
    operator fun <X> plus(x: X): Tuple4<E1, E2, E3, X> = tuple(e1, e2, e3, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple4<E1, E2, E3, X> = this + x
}

/** A tuple of four elements. See [Tuple1] for the conventions shared by the whole family. */
data class Tuple4<E1, E2, E3, E4>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
) {
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4) }

    /** Returns a [Tuple5] with [x] appended. */
    operator fun <X> plus(x: X): Tuple5<E1, E2, E3, E4, X> = tuple(e1, e2, e3, e4, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple5<E1, E2, E3, E4, X> = this + x
}

/** A tuple of five elements. See [Tuple1] for the conventions shared by the whole family. */
data class Tuple5<E1, E2, E3, E4, E5>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
) {
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4, e5) }

    /** Returns a [Tuple6] with [x] appended. */
    operator fun <X> plus(x: X): Tuple6<E1, E2, E3, E4, E5, X> = tuple(e1, e2, e3, e4, e5, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple6<E1, E2, E3, E4, E5, X> = this + x
}

/** A tuple of six elements. See [Tuple1] for the conventions shared by the whole family. */
data class Tuple6<E1, E2, E3, E4, E5, E6>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
) {
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4, e5, e6) }

    /** Returns a [Tuple7] with [x] appended. */
    operator fun <X> plus(x: X): Tuple7<E1, E2, E3, E4, E5, E6, X> = tuple(e1, e2, e3, e4, e5, e6, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple7<E1, E2, E3, E4, E5, E6, X> = this + x
}

/** A tuple of seven elements. See [Tuple1] for the conventions shared by the whole family. */
data class Tuple7<E1, E2, E3, E4, E5, E6, E7>(
    val e1: E1,
    val e2: E2,
    val e3: E3,
    val e4: E4,
    val e5: E5,
    val e6: E6,
    val e7: E7,
) {
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4, e5, e6, e7) }

    /** Returns a [Tuple8] with [x] appended. */
    operator fun <X> plus(x: X): Tuple8<E1, E2, E3, E4, E5, E6, E7, X> = tuple(e1, e2, e3, e4, e5, e6, e7, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple8<E1, E2, E3, E4, E5, E6, E7, X> = this + x
}

/** A tuple of eight elements. See [Tuple1] for the conventions shared by the whole family. */
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
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4, e5, e6, e7, e8) }

    /** Returns a [Tuple9] with [x] appended. */
    operator fun <X> plus(x: X): Tuple9<E1, E2, E3, E4, E5, E6, E7, E8, X> = tuple(e1, e2, e3, e4, e5, e6, e7, e8, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple9<E1, E2, E3, E4, E5, E6, E7, E8, X> = this + x
}

/** A tuple of nine elements. See [Tuple1] for the conventions shared by the whole family. */
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
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4, e5, e6, e7, e8, e9) }

    /** Returns a [Tuple10] with [x] appended. */
    operator fun <X> plus(x: X): Tuple10<E1, E2, E3, E4, E5, E6, E7, E8, E9, X> =
        tuple(e1, e2, e3, e4, e5, e6, e7, e8, e9, x)

    /** Non-operator alias for [plus]. */
    fun <X> append(x: X): Tuple10<E1, E2, E3, E4, E5, E6, E7, E8, E9, X> = this + x
}

/**
 * A tuple of ten elements - the largest arity, hence no `plus` / `append`.
 *
 * See [Tuple1] for the conventions shared by the whole family.
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
    /** The elements as a list, in declaration order, computed on first access. */
    val asList by lazy(LazyThreadSafetyMode.NONE) { listOf(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10) }
}
