package de.peekandpoke.kraft.utils

// TODO: move to ultra::common

fun <T> List<T>.setAt(idx: Int, item: T): List<T> {
    return replaceAt(idx, item)
}

fun <T> List<T>.modifyAt(idx: Int, modifier: (T) -> T): List<T> {
    return replaceAt(idx, modifier(this[idx]))
}

fun <T> List<T>.replaceAt(idx: Int, new: T): List<T> {
    return toMutableList().apply { this[idx] = new }.toList()
}

fun <T> List<T>.removeAt(idx: Int): List<T> {
    return toMutableList().apply { removeAt(idx) }.toList()
}

fun <T> List<T>.swap(idx1: Int, idx2: Int): List<T> {
    val mutable = toMutableList()
    val tmp = mutable[idx1]
    mutable[idx1] = mutable[idx2]
    mutable[idx2] = tmp
    return mutable.toList()
}
