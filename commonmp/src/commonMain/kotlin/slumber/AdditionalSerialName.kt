package de.peekandpoke.ultra.slumber

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Repeatable
annotation class AdditionalSerialName(val value: String)
