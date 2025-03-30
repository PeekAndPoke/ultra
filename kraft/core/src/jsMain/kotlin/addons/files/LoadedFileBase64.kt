package de.peekandpoke.kraft.addons.files

import org.w3c.files.File

data class LoadedFileBase64(
    val file: File,
    val dataUrl: String?,
    val mimeType: String?,
    val dataBase64: String?,
) {
    companion object {
        fun of(file: File, dataUrl: String?): LoadedFileBase64 {

            if (dataUrl == null) {
                return LoadedFileBase64(file = file, dataUrl = null, mimeType = null, dataBase64 = null)
            }

            try {
                val parts = dataUrl.split(',')
                // Get the mime component
                val mimeType = parts[0].split(':')[1].split(';')[0]
                // Get the content
                val dataBase64 = parts[1]

                return LoadedFileBase64(
                    file = file,
                    dataUrl = dataUrl,
                    mimeType = mimeType,
                    dataBase64 = dataBase64,
                )
            } catch (e: Throwable) {
                return LoadedFileBase64(file = file, dataUrl = dataUrl, mimeType = null, dataBase64 = null)
            }
        }
    }
}
