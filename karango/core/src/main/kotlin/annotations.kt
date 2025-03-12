package de.peekandpoke.karango

import de.peekandpoke.ultra.slumber.Slumber
import kotlin.annotation.AnnotationTarget.*

@Target(CLASS)
annotation class Karango {

    @Target(FIELD, PROPERTY, FUNCTION)
    @Slumber.Field
    annotation class Field

    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Ignore
}
