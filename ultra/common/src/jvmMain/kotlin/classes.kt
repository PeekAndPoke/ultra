package io.peekandpoke.ultra.common

import java.io.File
import kotlin.reflect.KClass

/**
 * Computes the relative file-system path from this class's package to [that] class's package.
 *
 * For example, from `com.a.b` to `com.a.c.d` the result is `../c/d`.
 */
fun KClass<*>.getRelativePackagePath(that: KClass<*>): File {

    return this.java.`package`.name.getRelativePackagePath(
        that.java.`package`.name
    )
}

internal fun String.getRelativePackagePath(that: String): File {

    val thisParts = this.split(".").filter { it.isNotEmpty() }.toMutableList()
    val thatParts = that.split(".").filter { it.isNotEmpty() }.toMutableList()

    var match = true

    while (match) {
        match = thisParts.isNotEmpty() && thatParts.isNotEmpty() && thisParts.first() == thatParts.first()

        if (match) {
            thisParts.shift()
            thatParts.shift()
        }
    }

    val path = thisParts.map { ".." }.plus(thatParts)

    return File(path.joinToString(File.separator))
}
