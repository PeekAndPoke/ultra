package de.peekandpoke.funktor.rest.codegen.dart

import de.peekandpoke.funktor.rest.codegen.Taggable
import de.peekandpoke.funktor.rest.codegen.Tags

interface DartElement : DartPrintable {

    val tags: Tags

    interface Definition : Taggable {
        fun implement(): DartElement
    }
}


interface DartFileElement : DartElement {

    val file: DartFile.Definition

    val info: String get() = ""

    interface Definition : DartElement.Definition {

        val file: DartFile.Definition

        override fun implement(): DartFileElement
    }
}

interface DartClassElement : DartElement {
    interface Definition : DartElement.Definition {
        override fun implement(): DartClassElement
    }
}
