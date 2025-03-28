package de.peekandpoke.ultra.common.fixture

object LoremCat {
    operator fun invoke(width: Int, height: Int, uuid: String? = null) = imageUrl(
        width = width,
        height = height,
        uuid = uuid,
    )

    fun imageUrl(width: Int, height: Int, uuid: String? = null): String {
        return when (uuid) {
            null -> "https://api.images.cat/$width/$height"
            else -> "https://api.images.cat/$width/$height/$uuid"
        }
    }

    fun imageUrls(amount: Int, width: Int, height: Int): List<String> {
        return (0 until amount).map { imageUrl(width, height) }
    }
}
