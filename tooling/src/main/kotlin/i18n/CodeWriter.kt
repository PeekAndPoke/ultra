package io.peekandpoke.ultra.tooling.i18n

/** A tiny indentation-aware source writer (4-space indents). */
class CodeWriter {
    private val sb = StringBuilder()
    private var depth = 0

    /** Appends [text] at the current indent, then a newline. Empty [text] emits a blank line. */
    fun line(text: String = ""): CodeWriter {
        if (text.isNotEmpty()) {
            repeat(depth) { sb.append("    ") }
            sb.append(text)
        }
        sb.append('\n')
        return this
    }

    /** Runs [body] one indent level deeper. */
    fun indented(body: CodeWriter.() -> Unit): CodeWriter {
        depth++
        body()
        depth--
        return this
    }

    override fun toString(): String = sb.toString()
}
