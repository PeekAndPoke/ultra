package de.peekandpoke.kraft.addons.files

import kotlinx.coroutines.asDeferred
import org.w3c.files.FileList
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.js.Promise

suspend fun FileList.loadAllAsBase64(): List<LoadedFileBase64> {

    val fileList = this

    val promises = (0 until fileList.length)
        .map { idx ->

            Promise<LoadedFileBase64> { resolve, _ ->
                val file = fileList[idx]!!
//                console.log(file)

                val reader = FileReader()

                var result = LoadedFileBase64(file = file, dataUrl = null, mimeType = null, dataBase64 = null)

                // wait for the file to load
                reader.onload = { loadEvt ->
                    (loadEvt.target.asDynamic().result as String).let { content ->
                        result = LoadedFileBase64.of(file, content)
                    }
                }
                // In case of error we return empty content
                reader.onloadend = { resolve(result) }

                // Start reading
                reader.readAsDataURL(file)
            }
        }

    return Promise.all(promises.toTypedArray()).asDeferred().await().toList()
}
