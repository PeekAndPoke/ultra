package io.peekandpoke.ultra.codegen

import com.github.difflib.DiffUtils
import io.kotest.assertions.fail

/**
 * Compares generated code against an expected block, ignoring blank lines and trailing whitespace.
 *
 * Carried over from the deleted Dart generator's test helper. Two changes: it no longer strips a
 * splash banner (there is none), and it splits on `\n` rather than the platform separator, because
 * [io.peekandpoke.ultra.codegen.printer.CodePrinter] always emits `\n`.
 */
infix fun String.shouldHaveNoDiffs(expected: String) {
    val actualNormalized = normalizeForDiff()
    val expectedNormalized = expected.normalizeForDiff()

    val deltas = DiffUtils.diff(actualNormalized, expectedNormalized).deltas

    if (deltas.isNotEmpty()) {
        fail(
            buildString {
                appendLine("Generated code did not match.")
                appendLine()
                appendLine("Expected:")
                appendLine(expectedNormalized.joinToString("\n"))
                appendLine()
                appendLine("Actual:")
                appendLine(actualNormalized.joinToString("\n"))
                appendLine()
                appendLine("Deltas:")
                deltas.forEach { appendLine(it.toString()) }
            }
        )
    }
}

private fun String.normalizeForDiff(): List<String> = split("\n")
    .map { it.trimEnd() }
    .filter { it.isNotEmpty() }
