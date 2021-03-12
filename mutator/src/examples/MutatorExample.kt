package de.peekandpoke.ultra.mutator.examples

import de.peekandpoke.ultra.common.docs.SimpleExample
import java.io.File
import kotlin.reflect.KClass

abstract class MutatorExample : SimpleExample() {

    fun KClass<*>.loadMutatorCode(): String {

        val pack = java.`package`.name.replace(".", File.separator)
        val fileName = "${simpleName}${"$$"}mutator.kt"

        val path = File("build/generated/source/kaptKotlin/test/$pack/$fileName").absoluteFile

        return path.readText()
    }
}
