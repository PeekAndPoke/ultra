package io.peekandpoke.ultra.html

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldStartWith

class PlaceholdersSpec : FreeSpec() {

    init {
        "Transparent2x1PngBase64" - {

            "Starts with data:image/png" {

                Transparent2x1PngBase64 shouldStartWith "data:image/png"
            }
        }
    }
}
