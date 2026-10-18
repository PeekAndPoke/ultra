package io.peekandpoke.funktor.insights.gui

import io.peekandpoke.funktor.insights.Insights
import io.peekandpoke.funktor.insights.collectors.TemplateInsightsCollector
import io.peekandpoke.funktor.staticweb.resources.css
import io.peekandpoke.funktor.staticweb.resources.lazyJs
import io.peekandpoke.funktor.staticweb.templating.SimpleTemplate
import kotlinx.html.div
import kotlinx.html.style
import kotlin.system.measureNanoTime

class InsightsRenderer(private val insights: Insights) {

    companion object {
        fun InsightsRenderer?.withInsights(template: SimpleTemplate, block: () -> Unit) {
            this?.render(template, block) ?: block()
        }
    }

    fun render(template: SimpleTemplate, block: () -> Unit) {
        val timing = measureNanoTime {
            with(template) {
                if (insights.config.enabled) {
                    styles {
                        css(webResources.insightsBar)
                    }
                    scripts {
                        lazyJs(webResources.insightsBar)
                    }
                    scripts {
                        div(classes = "insights-bar-placeholder") {
                            attributes["data-insights-filename"] = insights.getRequestDetailsUri() ?: ""
                            style = "display: none"
                        }
                    }
                }
            }

            block()
        }

        insights.getOrNull(TemplateInsightsCollector::class)?.record(timing)
    }
}
