package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.slumber.Slumber
import kotlin.annotation.AnnotationTarget.*

@Target(CLASS)
annotation class Vault {

    @Slumber.Field
    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Field

    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Ignore
}
