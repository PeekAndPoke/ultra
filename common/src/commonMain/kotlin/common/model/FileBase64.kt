package de.peekandpoke.ultra.common.model

import kotlinx.serialization.Serializable

@Serializable
data class FileBase64(
    /** The base 64 encoded data of the file */
    val data: String,
    /** The mime type of the file */
    val mimeType: String? = null,
) {
    companion object {
        private val dataUrlRegex
            get() =
                "^(data:)([\\w/+-]*)(;charset=[\\w-]+|;base64)?,(.*)".toRegex()

        fun fromDataUrl(dataUrl: String): FileBase64? {

            return dataUrlRegex.find(dataUrl)?.let { parsed ->
                val mimeType = parsed.groupValues[2].takeIf { it.isNotBlank() }
                val data = parsed.groupValues[4]

                FileBase64(
                    data = data,
                    mimeType = mimeType,
                )
            }
        }
    }

    fun asDataUrl(): String {
        return "data:${mimeType ?: ""};base64,$data"
    }
}
