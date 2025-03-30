@file:Suppress("Detekt:EnumNaming", "Detekt:VariableNaming")

package de.peekandpoke.kraft.semanticui

@Suppress("EnumEntryName", "unused")
enum class SemanticColor {
    none,
    default,
    white,
    red,
    orange,
    yellow,
    olive,
    green,
    teal,
    blue,
    violet,
    purple,
    pink,
    brown,
    grey,
    black;

    val isSet get() = this != default && this != none

    infix fun or(other: SemanticColor) = when (this) {
        none -> other
        else -> this
    }
}
