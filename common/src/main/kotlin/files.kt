package de.peekandpoke.ultra.common

import java.io.File

fun File.ensureDirectory() = apply {
    if (!exists()) {
        mkdirs()
    }
}
