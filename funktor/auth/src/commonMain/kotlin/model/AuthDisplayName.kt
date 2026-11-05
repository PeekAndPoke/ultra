package io.peekandpoke.funktor.auth.model

/**
 * The rule for the display name chosen on the activation page, shared by the page and the server so they cannot
 * disagree.
 *
 * Refused: blank, longer than [MAX_LENGTH] after trimming, control characters, line breaks, every invisible format
 * character (bidi controls and marks, zero-width spaces, word joiners), the tag block, which can carry hidden text,
 * and the letters that render blank. Kept: the zero-width joiner and non-joiner, which some scripts and emoji need.
 */
object AuthDisplayName {
    /** The longest accepted name, in UTF-16 chars after trimming. */
    const val MAX_LENGTH = 100

    /** What the page shows when [normalizeOrNull] refuses. */
    const val DESCRIPTION = "Please enter a name of 1 to $MAX_LENGTH characters."

    /** The trimmed name, or null when it breaks the rule. */
    fun normalizeOrNull(raw: String): String? {
        val name = raw.trim()

        return name.takeIf {
            it.isNotEmpty() && it.length <= MAX_LENGTH && it.none(::isRefused) && !it.containsTagCharacter()
        }
    }

    private const val ZERO_WIDTH_NON_JOINER = 0x200C
    private const val ZERO_WIDTH_JOINER = 0x200D

    /** Letters that render as nothing: the Hangul fillers and the blank braille cell. */
    private val BLANK_LETTERS = setOf(0x115F, 0x1160, 0x3164, 0xFFA0, 0x2800)

    private fun isRefused(c: Char): Boolean =
        c.isISOControl() ||
            c.category == CharCategory.LINE_SEPARATOR ||
            c.category == CharCategory.PARAGRAPH_SEPARATOR ||
            (c.category == CharCategory.FORMAT && c.code != ZERO_WIDTH_NON_JOINER && c.code != ZERO_WIDTH_JOINER) ||
            c.code in BLANK_LETTERS

    /** U+E0000..U+E007F, as UTF-16: the high surrogate 0xDB40 before a low one in 0xDC00..0xDC7F. */
    private fun String.containsTagCharacter(): Boolean =
        zipWithNext().any { (high, low) -> high.code == 0xDB40 && low.code in 0xDC00..0xDC7F }
}
