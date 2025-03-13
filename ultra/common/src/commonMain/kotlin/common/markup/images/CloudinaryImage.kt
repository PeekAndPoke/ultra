package de.peekandpoke.ultra.common.markup.images

data class CloudinaryImage(
    val protocol: String,
    val appId: String,
    val quality: Map<String, String?>,
    val transform: Map<String, String?>,
    val rest: List<String>
) {
    val url
        get(): String {

            val parts: List<String> = listOf(
                appId,
                "image/upload",
                quality.map {
                    when (it.value != null) {
                        true -> "${it.key}:${it.value}"
                        else -> it.key
                    }
                }.joinToString(","),

                transform.map {
                    when (it.value != null) {
                        true -> "${it.key}:${it.value}"
                        else -> it.key
                    }
                }.joinToString(","),

                *rest.toTypedArray()
            ).filter { it.isNotBlank() }

            return protocol + "://" + parts.joinToString("/")
        }

    fun autoDpr() = copy(
        quality = quality.plus(DPR_AUTO to null)
    )

    fun autoFormat() = copy(
        quality = quality.plus(F_AUTO to null)
    )

    fun autoQuality() = copy(
        quality = quality.plus(Q_AUTO to null)
    )

    fun transformWidth(width: Int) = copy(
        transform = transform.plus("w_$width" to null)
    )

    companion object {

        /**
         * Lets cloudinary adjust to the device pixel ratio ... see https://cloudinary.com/blog/automatic_responsive_images_with_client_hints
         */
        private const val DPR_AUTO = "dpr_auto"

        /**
         * Lets cloudinary choose the image format: https://cloudinary.com/blog/adaptive_browser_based_image_format_delivery
         */
        private const val F_AUTO = "f_auto"

        /**
         * Lets cloudinary choose the quality: https://cloudinary.com/documentation/image_optimization
         */
        private const val Q_AUTO = "q_auto"

        private val urlRegex = "(.*)://(.*)cloudinary\\.com/(.*)/image/upload/(.*)".toRegex()

        private val qualityKeywords = listOf(
            DPR_AUTO,
            F_AUTO,
            Q_AUTO
        )

        fun fromUrl(url: String): CloudinaryImage? {

            val match = urlRegex.find(url) ?: return null

            val protocol = match.groups[1]!!.value
            val appId = match.groups[2]!!.value + "cloudinary.com/" + match.groups[3]!!.value
            val parts = match.groups[4]!!.value.split("/")

            if (parts.isEmpty()) {
                return null
            }

            return when (qualityKeywords.any { parts[0].contains(it) }) {
                true -> CloudinaryImage(
                    protocol = protocol,
                    appId = appId,
                    quality = parts[0].split(",").map { option ->
                        option.split(":").let {
                            when (it.size) {
                                2 -> it[0] to it[1]
                                else -> option to null
                            }
                        }
                    }.toMap(),
                    transform = emptyMap(),
                    rest = parts.drop(1)
                )

                else -> CloudinaryImage(
                    protocol = protocol,
                    appId = appId,
                    quality = emptyMap(),
                    transform = emptyMap(),
                    rest = parts
                )
            }
        }
    }
}
