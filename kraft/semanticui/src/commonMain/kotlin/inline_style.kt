package de.peekandpoke.kraft.semanticui

import kotlinx.css.CssBuilder
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.style

data class InlineStyle(val css: String)

fun inlineStyle(block: CssBuilder.() -> Unit): InlineStyle = InlineStyle(CssBuilder().apply(block).toString())

fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) = css(CssBuilder().apply(block))

fun CommonAttributeGroupFacade.css(builder: CssBuilder) {
    style = builder.toString()
}

fun CommonAttributeGroupFacade.css(css: InlineStyle) {
    style = css.css
}

