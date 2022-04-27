package de.peekandpoke.ultra.semanticui

@Suppress("EnumEntryName")
enum class SemanticNumber {
    one,
    two,
    three,
    four,
    five,
    six,
    seven,
    eight,
    nine,
    ten,
    eleven,
    twelve,
    thirteen,
    fourteen,
    fifteen,
    sixteen;

    companion object {
        fun of(num: Int): SemanticNumber = when {
            num <= 1 -> one
            num == 2 -> two
            num == 3 -> three
            num == 4 -> four
            num == 5 -> five
            num == 6 -> six
            num == 7 -> seven
            num == 8 -> eight
            num == 9 -> nine
            num == 10 -> ten
            num == 11 -> eleven
            num == 12 -> twelve
            num == 13 -> thirteen
            num == 14 -> fourteen
            num == 15 -> fifteen
            else -> sixteen
        }
    }
}
