package io.peekandpoke.ultra.slumber

/**
 * Registers an additional, READ-ONLY serial name for a polymorphic child.
 *
 * Awaking accepts the alias as a type discriminator; slumbering always writes the primary identifier
 * (`Polymorphic.Child.identifier`, else `@SerialName`, else the qualified class name) and never an
 * alias. That is what makes it a rename-compatibility tool: old data keeps deserializing while new
 * data is written under the new name. Repeatable, so a type may carry several aliases.
 */
// TODO(scan): AnnotationTarget.PROPERTY is declared but never honoured — the only reader
//  (PolymorphicChildUtil.getAdditionalIdentifiers) inspects class annotations, so a property-level
//  @AdditionalSerialName compiles and is silently ignored.
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Repeatable
annotation class AdditionalSerialName(val value: String)
