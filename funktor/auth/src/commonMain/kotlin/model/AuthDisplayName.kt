package io.peekandpoke.funktor.auth.model

/**
 * The rule for the display name chosen on the activation page, shared by the page and the server so they cannot
 * disagree.
 *
 * After trimming, a name is 1 to [MAX_LENGTH] UTF-16 chars on one line, built only of characters that show:
 * letters, marks, digits, punctuation, symbols and inner spaces. Refused, with a [Problem] that says so: control
 * characters and line breaks; invisible characters (format controls, the tag block, default-ignorable code points,
 * the letters that render blank); characters that cannot be displayed (private use, noncharacters, broken UTF-16);
 * a leading combining mark, more than 30 marks in a row. Kept, as scripts and emoji need them: the zero-width joiner
 * and non-joiner between two visible characters (or after an Indic virama), one variation selector after a base
 * character, an ideographic one only after a CJK ideograph.
 *
 * Beyond the Basic Multilingual Plane, common code has no Unicode categories: a code point there is refused by the
 * ranges below, otherwise taken as visible.
 */
object AuthDisplayName {
    /** The longest accepted name, in UTF-16 chars after trimming. */
    const val MAX_LENGTH = 100

    /** The rule in one sentence, for a refusal the page cannot explain from its own input. */
    const val DESCRIPTION = "Please enter a name of 1 to $MAX_LENGTH visible characters, on one line."

    /** Why a name is refused. */
    enum class Problem {
        Blank,
        TooLong,
        LineBreak,
        Invisible,
        NotDisplayable,
        StartsWithMark,
        TooManyMarks,
    }

    /** A refused name: the [problem], the offending [codePoint] if there is one, and the [message] for the user. */
    data class Refusal(val problem: Problem, val codePoint: Int? = null, val position: Position? = null) {
        /** Where [codePoint] sits in the trimmed name. */
        enum class Position { Start, Inside, End }

        val message: String
            get() = when (problem) {
                Problem.Blank -> "Please enter your name."
                Problem.TooLong -> "Please use at most $MAX_LENGTH characters (an emoji counts as two or more)."
                Problem.LineBreak ->
                    "Please keep your name on one line, without tabs, line breaks or control characters."
                Problem.Invisible ->
                    "Your name contains an invisible character (${codePointText()})${positionText()}. " +
                    "It often comes along with copy and paste - please delete it, or type your name in again."
                Problem.NotDisplayable -> "Your name contains a character that cannot be displayed " +
                    "(${codePointText()})${positionText()}. Please type your name in again."
                Problem.StartsWithMark -> "Your name starts with an accent or mark (${codePointText()}) that belongs " +
                    "on a letter. Please type your name in again."
                Problem.TooManyMarks -> "Your name stacks more than $MAX_MARKS_IN_A_ROW accents or marks on one " +
                    "letter${positionText()}. Please type your name in again."
            }

        private fun codePointText(): String =
            codePoint?.let { "U+" + it.toString(16).uppercase().padStart(4, '0') } ?: "?"

        private fun positionText(): String = when (position) {
            Position.Start -> " at the start"
            Position.End -> " at the end"
            Position.Inside, null -> ""
        }
    }

    /** The trimmed name, or null when it breaks the rule. */
    fun normalizeOrNull(raw: String): String? = raw.trim().takeIf { refusalOf(it) == null }

    /** Why [raw] breaks the rule, or null when it is accepted. */
    fun refusalOf(raw: String): Refusal? {
        val name = raw.trim()

        if (name.isEmpty()) return Refusal(Problem.Blank)
        if (name.length > MAX_LENGTH) return Refusal(Problem.TooLong)

        val codePoints = name.codePointList()
        var marksInARow = 0

        codePoints.forEachIndexed { i, cp ->
            val prev = codePoints.getOrNull(i - 1)
            val next = codePoints.getOrNull(i + 1)
            val position = when (i) {
                0 -> Refusal.Position.Start
                codePoints.lastIndex -> Refusal.Position.End
                else -> Refusal.Position.Inside
            }

            fun refuse(problem: Problem) = Refusal(problem, cp, position)

            when {
                cp.isControlOrSeparator() -> return refuse(Problem.LineBreak)
                cp.isNotDisplayable() -> return refuse(Problem.NotDisplayable)
                cp == ZERO_WIDTH_NON_JOINER || cp == ZERO_WIDTH_JOINER -> {
                    // After a virama, a joiner may end a word: older Bengali and Malayalam spelling.
                    val joinsBoth = next != null && next.isVisible()
                    if (prev == null || !prev.isVisible() || !(joinsBoth || prev in VIRAMAS)) {
                        return refuse(Problem.Invisible)
                    }
                }

                // An ideographic variation selector only on an ideograph: on any other letter it changes nothing,
                // and one per letter would carry hidden bytes.
                cp.isIdeographicVariationSelector() ->
                    if (prev == null || !prev.isIdeograph()) return refuse(Problem.Invisible)

                cp.isVariationSelector() ->
                    if (prev == null || !prev.isBase()) return refuse(Problem.Invisible)

                cp.isInvisible() -> return refuse(Problem.Invisible)
                cp.isMark() -> {
                    if (prev == null) return refuse(Problem.StartsWithMark)
                    if (++marksInARow > MAX_MARKS_IN_A_ROW) return refuse(Problem.TooManyMarks)
                }
            }

            if (!cp.isMark()) marksInARow = 0
        }

        return null
    }

    /** The stream-safe limit of UAX #15: more marks on one letter spill over the lines around the name. */
    private const val MAX_MARKS_IN_A_ROW = 30

    private const val ZERO_WIDTH_NON_JOINER = 0x200C
    private const val ZERO_WIDTH_JOINER = 0x200D

    /** Code points that render as nothing but are not format characters: fillers, blank braille, ignorables. */
    private val BLANK_CODE_POINTS = setOf(0x034F, 0x115F, 0x1160, 0x17B4, 0x17B5, 0x2065, 0x2800, 0x3164, 0xFFA0)

    /** The Indic viramas a word-final joiner may follow. */
    private val VIRAMAS = setOf(0x094D, 0x09CD, 0x0A4D, 0x0ACD, 0x0B4D, 0x0BCD, 0x0C4D, 0x0CCD, 0x0D4D, 0x0DCA)

    /** Format controls and default-ignorable code points beyond the BMP; variation selectors are handled apart. */
    private val INVISIBLE_SUPPLEMENTARY = listOf(
        0x110BD..0x110BD,
        0x110CD..0x110CD,
        0x13430..0x1343F,
        0x1BCA0..0x1BCA3,
        0x1D173..0x1D17A,
        0xE0000..0xE00FF, // the tag block, which can carry hidden text, and its unassigned neighbours
        0xE01F0..0xE0FFF,
    )

    /** The name's code points; a surrogate half without its partner stays in as itself, to be refused. */
    private fun String.codePointList(): List<Int> {
        val result = ArrayList<Int>(length)
        var i = 0

        while (i < length) {
            val c = this[i]
            val low = getOrNull(i + 1)

            if (c.isHighSurrogate() && low != null && low.isLowSurrogate()) {
                result.add(0x10000 + ((c.code - 0xD800) shl 10) + (low.code - 0xDC00))
                i += 2
            } else {
                result.add(c.code)
                i += 1
            }
        }

        return result
    }

    private fun Int.category(): CharCategory? = if (this < 0x10000) Char(this).category else null

    private fun Int.isControlOrSeparator(): Boolean = category().let {
        it == CharCategory.CONTROL || it == CharCategory.LINE_SEPARATOR || it == CharCategory.PARAGRAPH_SEPARATOR
    }

    private fun Int.isNotDisplayable(): Boolean =
        category() == CharCategory.PRIVATE_USE ||
            this in 0xD800..0xDFFF || // a surrogate half without its partner
            this >= 0xF0000 || // the private-use planes
            (this and 0xFFFE) == 0xFFFE || // the two noncharacters at the end of every plane
            this in 0xFDD0..0xFDEF ||
            this in 0xFFF0..0xFFF8

    private fun Int.isIdeographicVariationSelector(): Boolean = this in 0xE0100..0xE01EF

    /** The CJK unified and compatibility ideographs, the BMP blocks and the supplementary planes 2 and 3. */
    private fun Int.isIdeograph(): Boolean =
        this in 0x3400..0x4DBF || this in 0x4E00..0x9FFF || this in 0xF900..0xFAFF || this in 0x20000..0x3FFFF

    private fun Int.isVariationSelector(): Boolean =
        this in 0xFE00..0xFE0F || this in 0xE0100..0xE01EF || this in 0x180B..0x180D || this == 0x180F

    private fun Int.isInvisible(): Boolean =
        category() == CharCategory.FORMAT || this in BLANK_CODE_POINTS || INVISIBLE_SUPPLEMENTARY.any { this in it }

    private fun Int.isMark(): Boolean = category().let {
        it == CharCategory.NON_SPACING_MARK || it == CharCategory.ENCLOSING_MARK ||
            it == CharCategory.COMBINING_SPACING_MARK
    }

    private fun Int.isSpace(): Boolean = category() == CharCategory.SPACE_SEPARATOR

    /** Anything that shows on its own: not a space, joiner, variation selector, mark or invisible character. */
    private fun Int.isBase(): Boolean =
        !isSpace() && !isMark() && !isVariationSelector() && !isInvisible() &&
            this != ZERO_WIDTH_NON_JOINER && this != ZERO_WIDTH_JOINER &&
            !isControlOrSeparator() && !isNotDisplayable()

    /** What a joiner may sit between: a base, or a mark or variation selector on one. */
    private fun Int.isVisible(): Boolean = isBase() || isMark() || isVariationSelector()
}
