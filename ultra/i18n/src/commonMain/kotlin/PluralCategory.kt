package io.peekandpoke.ultra.i18n

/**
 * CLDR plural categories. For now only [ONE] / [OTHER] are ever selected, using a simple Germanic
 * rule (`count == 1` -> [ONE], else [OTHER]). Full, locale-aware CLDR selection is deferred (see the
 * i18n plan, D9) — hence the [Locale] seam on [pluralCategoryFor].
 */
enum class PluralCategory(val suffix: String) {
    ZERO("zero"),
    ONE("one"),
    TWO("two"),
    FEW("few"),
    MANY("many"),
    OTHER("other"),
}

/** Selects a plural category for [count]. Simple English/German rule until CLDR lands (D9). */
@Suppress("UNUSED_PARAMETER")
fun pluralCategoryFor(count: Int, locale: Locale): PluralCategory =
    if (count == 1) PluralCategory.ONE else PluralCategory.OTHER
