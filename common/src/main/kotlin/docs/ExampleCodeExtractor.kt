package de.peekandpoke.ultra.common.docs

import java.io.File

object ExampleCodeExtractor {

    private const val beginPattern = "// !BEGIN! //"
    private const val endPattern = "// !END! //"

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
            val begin = src.indexOf(beginPattern, idx)
            val end = src.indexOf(endPattern, idx)
            val found = begin != -1 && end != -1

            if (found) {
                result
                    .appendln(src.substring(begin + beginPattern.length, end).trimIndent().trim())
                    .appendln()

                idx = end + endPattern.length
            }

        } while (found)

        return result.toString().trim()
    }
}
