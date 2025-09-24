package de.peekandpoke.funktor.insights

import de.peekandpoke.funktor.insights.gui.InsightsBarTemplate
import de.peekandpoke.funktor.insights.gui.InsightsGuiTemplate
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.DIV
import kotlinx.html.Unsafe
import kotlinx.html.div
import kotlinx.html.script
import kotlinx.html.unsafe

interface InsightsCollectorData {

    val templateKey: String get() = (this::class.qualifiedName ?: "cls-${this::class.hashCode()}").toId()

    fun renderBar(template: InsightsBarTemplate) {}

    fun renderDetails(template: InsightsGuiTemplate) {}

    fun InsightsGuiTemplate.menu(block: DIV.() -> Unit) {

        menuPlaceholders {
            ui.item {
                attributes["data-key"] = templateKey

                block()
            }
        }
    }

    fun InsightsGuiTemplate.content(block: DIV.() -> Unit) {

        contentPlaceholders {
            div {
                attributes["data-key"] = templateKey

                ui.basic.segment {
                    block()
                }
            }
        }
    }

    fun InsightsGuiTemplate.inlineScript(block: Unsafe.() -> Unit) {

        scripts {
            script {
                unsafe {
                    +"(() => {"

                    block()

                    +"})();"
                }
            }
        }
    }

    private fun String.toId() = replace("[^a-zA-Z0-9]".toRegex(), "-")

    fun Long.formatMs(precision: Int = 2) = "${"%.${precision}f".format(this / 1_000_000.0)} ms"
}
