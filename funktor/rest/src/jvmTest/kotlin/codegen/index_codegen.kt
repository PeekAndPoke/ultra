package de.peekandpoke.ktorfx.rest.codegen

import com.github.difflib.DiffUtils
import io.kotest.assertions.fail

infix fun String.shouldHaveNoDiffs(expected: String) {

    val thisNormalized = this.normalizeForDiff()
    val expectedNormalized = expected.normalizeForDiff()

    val diffs = DiffUtils.diff(thisNormalized, expectedNormalized, null)

    if (diffs.deltas.isNotEmpty()) {

        val clue = "Expected:\n\n" + expectedNormalized + "\n\n" +
                "Actual:\n\n" + thisNormalized + "\n\n" +
                "Deltas:\n\n" + diffs.deltas.toString()

        fail(clue)
    }
}

private val infoRegex = "// \\[INFO].*".toRegex()

private val lineFilters = listOf<(String) -> Boolean>(
    { it.trim().isNotEmpty() },
    { !it.matches(infoRegex) },
)

/**
 * Normalizes code by trimming line ends.
 *
 * This is handy for have a more relaxed comparison of code blocks.
 */
private fun String.normalizeForDiff(delimiter: String = System.lineSeparator()) =
    replace(splash, "")
        .split(delimiter)
        .filter { line ->
            lineFilters.all { filter -> filter(line) }
        }
        .joinToString(delimiter) { it.trimEnd() }
