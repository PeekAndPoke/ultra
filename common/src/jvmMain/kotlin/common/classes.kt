package de.peekandpoke.ultra.common

import java.io.File
import kotlin.reflect.KClass

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
