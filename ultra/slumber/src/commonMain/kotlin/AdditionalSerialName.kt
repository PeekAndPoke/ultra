package de.peekandpoke.ultra.slumber

/**
 * Provides an additional serial name alias for polymorphic type discrimination.
 *
 * Use this for backwards compatibility when renaming types — old discriminator values
 * will still deserialize correctly.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Repeatable
annotation class AdditionalSerialName(val value: String)
