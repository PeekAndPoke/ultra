package de.peekandpoke.ultra.slumber

import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.PROPERTY

interface Slumber {

    @Target(FIELD, PROPERTY)
    annotation class Field
}
