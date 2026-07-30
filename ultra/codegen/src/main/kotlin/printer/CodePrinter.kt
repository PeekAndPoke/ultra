package io.peekandpoke.ultra.codegen.printer

/**
 * Accumulates generated source text with automatic indentation.
 *
 * Always emits `\n`, never the platform line separator — generated files must be byte-identical
 * across operating systems so golden-file tests and `--check` stay stable.
 */
class CodePrinter(
    private val indentUnit: String = "    ",
) {
    private val sb = StringBuilder()

    private var level = 0

    /** True while the current line is still empty, i.e. indentation has not been written yet. */
    private var pendingIndent = true

    companion object {
        const val NL: String = "\n"

        /** Runs [block] on a fresh printer and returns the produced text. */
        fun print(block: CodePrinter.() -> Unit): String = CodePrinter().apply(block).build()
    }

    /** The generated text. */
    fun build(): String = sb.toString()

    /**
     * Appends [text], indenting every line it spans.
     *
     * Multi-line input is split so that a raw block (a license header, a hand-written snippet) picks
     * up the current indentation instead of flattening against the left margin.
     */
    fun append(text: String): CodePrinter = apply {
        if (text.isEmpty()) return@apply

        val lines = text.split(NL)

        lines.forEachIndexed { idx, line ->
            if (idx > 0) {
                newLine()
            }

            if (line.isNotEmpty()) {
                writeIndentIfPending()
                sb.append(line)
            }
        }
    }

    /** Appends [text] followed by a line break. */
    fun appendLine(text: String = ""): CodePrinter = append(text).nl()

    /** Appends [count] line breaks. */
    fun nl(count: Int = 1): CodePrinter = apply {
        repeat(count) { newLine() }
    }

    /**
     * Appends each of [items] via [each], writing [separator] between consecutive items.
     *
     * The separator does not run after the last item, so a trailing comma or blank line never leaks
     * into the output.
     */
    fun <T> appendEach(
        items: Iterable<T>,
        separator: CodePrinter.() -> Unit = { nl() },
        each: CodePrinter.(T) -> Unit,
    ): CodePrinter = apply {
        var first = true

        items.forEach { item ->
            if (!first) {
                separator()
            }
            first = false
            each(item)
        }
    }

    /**
     * Runs [block] one indentation level deeper.
     *
     * A line break is emitted before and after the block, so callers write
     * `append(" {").indented { … }.append("}")` and get a conventionally formatted body.
     */
    fun indented(block: CodePrinter.() -> Unit): CodePrinter = apply {
        level++
        nl()
        block()
        level--
        nl()
    }

    /** Runs [block] one indentation level deeper without adding surrounding line breaks. */
    fun indentedRaw(block: CodePrinter.() -> Unit): CodePrinter = apply {
        level++
        block()
        level--
    }

    private fun newLine() {
        sb.append(NL)
        pendingIndent = true
    }

    private fun writeIndentIfPending() {
        if (pendingIndent) {
            repeat(level) { sb.append(indentUnit) }
            pendingIndent = false
        }
    }
}
