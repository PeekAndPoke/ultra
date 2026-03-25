package de.peekandpoke.ultra.slumber

import kotlin.annotation.AnnotationTarget.*

/** Marker annotation for Slumber serialization. */
@Target()
annotation class Slumber {

    /** Marks a property or field for inclusion in serialization output, even if it is not a constructor parameter. */
    @Target(FIELD, PROPERTY, ANNOTATION_CLASS)
    annotation class Field
}
