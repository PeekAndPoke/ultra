package de.peekandpoke.ultra.semanticui

import kotlinx.css.Color

interface SemanticColorToColor {

    data class ColorAndName(
        val semantic: SemanticColor,
        val color: Color,
        val name: String,
    )

    val valid: List<ColorAndName>

    fun get(semantic: SemanticColor): ColorAndName

    operator fun invoke(semantic: SemanticColor): ColorAndName = get(semantic)
}
