package de.peekandpoke.ultra.common.fixture

import kotlin.random.Random

/**
 * TODO: test
 */
object LoremIpsum {

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

    private val rand = Random(1234)

    operator fun invoke(words: Int) = words(words)

    fun words(words: Int) = WORDS_SPLIT.subList(0, words).joinToString(" ")

    fun words(words: Int, randomRange: Int = 5) = words(words + rand.nextInt(-randomRange, randomRange))

    fun imageUrl(width: Int, height: Int): String = "https://picsum.photos/$width/$height?r=${rand.nextInt()}"

    fun imageUrls(amount: Int, width: Int, height: Int): List<String> = (0 until amount).map { imageUrl(width, height) }
}
