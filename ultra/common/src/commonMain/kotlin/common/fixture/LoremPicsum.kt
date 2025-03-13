package de.peekandpoke.ultra.common.fixture

import kotlin.random.Random

object LoremPicsum {
    private val rand = Random(1234)

    operator fun invoke(width: Int, height: Int) = imageUrl(width = width, height = height)

    fun imageUrl(width: Int, height: Int): String {
        return "https://picsum.photos/$width/$height?r=${rand.nextInt()}"
    }

    fun imageUrls(amount: Int, width: Int, height: Int): List<String> {
        return (0 until amount).map { imageUrl(width, height) }
    }
}
