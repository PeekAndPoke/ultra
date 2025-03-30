package de.peekandpoke.ktorfx.staticweb.semanticui.templates

import de.peekandpoke.ktorfx.staticweb.templating.SimpleTemplateBase
import de.peekandpoke.ktorfx.staticweb.templating.TemplateTools
import io.ktor.server.html.*
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.meta

open class SemanticUiPlainTemplate(
    tools: TemplateTools,
) : SimpleTemplateBase(tools) {

    override fun render(html: HTML): Unit = with(html) {
        attributes.putAll(htmlAttributes)

        head {
            meta { charset = "utf-8" }
            each(pageHead) { insert(it) }
            each(styles) { insert(it) }
        }

        body {
            each(content) { insert(it) }
            each(scripts) { insert(it) }
        }
    }
}
