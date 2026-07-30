package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.slumber.Slumberer
import java.lang.reflect.Array as JavaArray

/** Serializes Iterables and arrays into a list of recursively slumbered elements. */
object CollectionSlumberer : Slumberer {

    /** Anything that is neither an [Iterable] nor an array slumbers to `null`. */
    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when {

        data == null -> null

        // TODO(scan): a one-shot Iterable (e.g. a DB cursor) is consumed here. Codec.slumberInternal
        //  retries the whole slumber on SlumberException, so the tracking pass re-reads an exhausted
        //  input and silently returns an EMPTY list instead of rethrowing.
        data is Iterable<*> -> map(data, context)

        // Covers Array<T> AND the eight primitive arrays. A plain `data is Array<*>` check would miss
        // IntArray and friends, because `int[]` is not an `Object[]`.
        data::class.java.isArray -> map(data.readArrayElements(), context)

        else -> null
    }

    /** Slumbers every element under its index as the path step. */
    private fun map(data: Iterable<*>, context: Slumberer.Context) = data.mapIndexed { idx, it ->
        context.stepInto(idx.toString()).slumber(it)
    }

    /** Reads any array — object or primitive — element by element, boxing primitives on the way out. */
    private fun Any.readArrayElements(): List<Any?> {
        val size = JavaArray.getLength(this)

        return (0 until size).map { JavaArray.get(this, it) }
    }
}
