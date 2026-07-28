package io.peekandpoke.ultra.vault

import io.peekandpoke.ultra.slumber.Slumber
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Marks a class as a Vault entity: the Karango and Monko KSP processors generate typed
 * property-path accessors for every annotated class, which is what the query DSLs build on.
 */
@Target(CLASS)
annotation class Vault {

    /**
     * Includes a NON-constructor property in the generated property paths.
     *
     * Meta-annotated with [Slumber.Field], so the property is serialized as well. Primary
     * constructor parameters are picked up automatically and need no annotation.
     */
    @Slumber.Field
    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Field

    /**
     * Excludes a NON-constructor property from the generated property paths.
     *
     * Has no effect on a primary constructor parameter. Those are always generated, because they are
     * also what the entity is rebuilt from — excluding one would break the serialization round trip.
     * Annotating a constructor parameter is silently ignored rather than reported.
     *
     * It never affects serialization either way.
     */
    @Target(FIELD, PROPERTY, FUNCTION)
    annotation class Ignore
}
