package de.peekandpoke.ultra.common.docs

import java.io.File

object ExampleCodeExtractor {

    private const val BEGIN = "// !BEGIN! //"
    private const val END = "// !END! //"

    fun extract(example: Example, srcDir: File): String {

        val srcFile = File(srcDir, "${example::class.simpleName}.kt")

        if (!srcFile.exists()) {
            return "Source file not found: $srcFile"
        }

        return extractFromExample(srcFile.readText())
    }

    private fun extractFromExample(src: String): String {

        var idx = 0
        val result = StringBuilder()

        do {
            val begin = src.indexOf(BEGIN, idx)
            val end = src.indexOf(END, idx)
            val found = begin != -1 && end != -1

            if (found) {
                result
                    .appendLine(src.substring(begin + BEGIN.length, end).trimIndent().trim())
                    .appendLine()

                idx = end + END.length
            }
        } while (found)

        return result.toString().trim()
    }
}
