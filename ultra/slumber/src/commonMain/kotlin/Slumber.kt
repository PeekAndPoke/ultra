package io.peekandpoke.ultra.slumber

import kotlin.annotation.AnnotationTarget.*

/**
 * Namespace for slumber-related annotations.
 *
 * `@Target()` is empty, so [Slumber] itself cannot be applied to any declaration — only nested
 * [Field] is actually used throughout the codebase.
 */
@Target()
annotation class Slumber {

    /**
     * Marks a property or field for inclusion in serialization output, even if it is not a constructor parameter.
     *
     * `ANNOTATION_CLASS` is a valid target too: other annotations can carry `@Slumber.Field` themselves to opt
     * their annotated properties in via meta-annotation (see `DataClassSlumberer.hasAnyAnnotationRecursive`).
     */
    @Target(FIELD, PROPERTY, ANNOTATION_CLASS)
    annotation class Field
}
