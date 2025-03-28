package de.peekandpoke.ultra.common

import java.io.File

fun File.ensureDirectory() = apply {
    if (!exists()) {
        mkdirs()
    }
}

fun File.cleanDirectory() = apply {
    deleteRecursively()
    ensureDirectory()
}

fun File.child(child: File) = File(this, child.path)
