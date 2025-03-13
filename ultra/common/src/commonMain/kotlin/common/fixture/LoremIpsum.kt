package de.peekandpoke.ultra.common.fixture

import kotlin.random.Random

/**
 * TODO: test
 */
object LoremIpsum {

    private val rand = Random(1234)

    private const val WORDS = "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, " +
            "sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, " +
            "sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. " +
            "Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. " +
            "Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod " +
            "tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. " +
            "At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, " +
            "no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, " +
            "consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore " +
            "magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. " +
            "Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet."

    private val WORDS_SPLIT = WORDS.split(" ")

    private fun getWordAt(idx: Int) = WORDS_SPLIT[idx % WORDS_SPLIT.size]

    operator fun invoke(words: Int) = words(words)

    fun words(words: Int) = (0..<words).joinToString(separator = " ", transform = ::getWordAt)

    fun words(words: Int, randomRange: Int = 5) = words(words + rand.nextInt(-randomRange, randomRange))
}
