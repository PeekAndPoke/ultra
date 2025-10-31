package de.peekandpoke.ultra.vault

import de.peekandpoke.ultra.slumber.Slumber
import kotlin.annotation.AnnotationTarget.*

@Target(CLASS)
annotation class Vault {

    @Slumber.Field
    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Field

    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Ignore
}
