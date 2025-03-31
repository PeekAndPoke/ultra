package de.peekandpoke.kraft.utils

import js.objects.Object

fun jsIsObject(o: dynamic): Boolean = jsTypeOf(o) == "object"

@Suppress("UNUSED_PARAMETER")
fun jsIsArray(o: dynamic): Boolean = js("(Array.isArray(o))") as Boolean

fun jsObject(): dynamic = js("({})")
fun jsArray(): dynamic = js("([])")

fun <T> jsObject(block: T.() -> Unit): T {
    val obj = jsObject() as T
    block(obj)
    return obj
}

fun jsObjectOf(vararg pairs: Pair<String, dynamic>): dynamic {
    val obj = jsObject()

    pairs.forEach { (k, v) -> obj[k] = v }

    return obj
}

fun jsObjectToMap(obj: dynamic): Map<String, Any?> {
    return Object.keys(obj as Any)
        .associateWith { jsToKotlin(obj[it]) }
}

fun jsArrayToList(arr: dynamic): List<Any?> {
    val result = mutableListOf<Any?>()

    arr.forEach { item ->
        result.add(jsToKotlin(item))
    }

    return result.toList()
}

fun jsToKotlin(it: dynamic): Any? {
    if (it == null) {
        return null
    }

    if (jsIsArray(it)) {
        return jsArrayToList(it)
    }

    if (jsIsObject(it)) {
        return jsObjectToMap(it)
    }

    return it
}

/**
 * Converts a map to a raw javascript object
 */
val <T> Map<String, T>.js
    get() : dynamic {
        val obj = jsObject()

        forEach { (k, v) -> obj[k] = v }

        return obj
    }

val <T> List<T>.js
    get(): dynamic {
        val arr = jsArray()

        @Suppress("UnsafeCastFromDynamic")
        forEach { arr.push(it) }

        return arr
    }

