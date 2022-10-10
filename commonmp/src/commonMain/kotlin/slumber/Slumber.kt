package de.peekandpoke.ultra.slumber

import kotlin.annotation.AnnotationTarget.*

@Target()
annotation class Slumber {

    @Target(FIELD, PROPERTY, ANNOTATION_CLASS)
    annotation class Field
}
