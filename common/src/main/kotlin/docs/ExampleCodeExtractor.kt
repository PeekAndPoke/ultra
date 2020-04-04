package de.peekandpoke.ultra.common.docs

import java.io.File

object ExampleCodeExtractor {

    private const val beginPattern = "// !BEGIN! //"
    private const val endPattern = "// !END! //"

    fun extract(example: Example, srcDir: File): String? {

        val srcFile = File(srcDir, "${example::class.simpleName}.kt")

        if (!srcFile.exists()) {
            return "Source file not found: $srcFile"
        }

        return extractFromExample(srcFile.readText())
    }

    private fun extractFromExample(src: String): String? {
        val begin = src.indexOf(beginPattern)
        val end = src.indexOf(endPattern)

        if (begin == -1 || end == -1) return null

        return src.substring(begin + beginPattern.length, end).trimIndent().trim()
    }
}
