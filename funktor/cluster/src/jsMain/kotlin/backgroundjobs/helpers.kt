package de.peekandpoke.funktor.cluster.backgroundjobs

import de.peekandpoke.ultra.common.datetime.formatDdMmmYyyyHhMmSs
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.semanticui.SemanticIconFn
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.css.Color
import kotlinx.css.Overflow
import kotlinx.css.Padding
import kotlinx.css.backgroundColor
import kotlinx.css.overflow
import kotlinx.css.padding
import kotlinx.css.px
import kotlinx.html.FlowContent
import kotlinx.html.br
import kotlinx.html.pre
import kotlinx.html.span
import kotlinx.html.title

internal fun FlowContent.renderTableEntry(results: List<BackgroundJobResultModel>) {
    results.asReversed()
        .chunked(10)
        .forEach { chunk ->
            chunk.forEach { result ->
                span {
                    title = """
                        ${result.executionTimeMs}ms at ${
                        result.executedAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs()
                    } by ${result.serverId}                    
                    """.trimIndent()

                    renderIcon(result)
                }
            }

            br {}
        }
}

internal fun FlowContent.renderIcon(result: BackgroundJobResultModel?, style: SemanticIconFn = { this }) {
    when (result) {
        is BackgroundJobResultModel.Success -> icon.green.style().check_circle()
        is BackgroundJobResultModel.Failed -> icon.red.style().times_circle()
        null -> icon.style().question_circle()
    }
}

internal fun FlowContent.renderTableEntry(policy: BackgroundJobRetryPolicyModel) {
    ui.small.divided.list {
        ui.item {
            renderAsListItemContent(policy)
        }
    }
}

internal fun FlowContent.renderAsListItemContent(policy: BackgroundJobRetryPolicyModel) {
    when (policy) {
        is BackgroundJobRetryPolicyModel.None -> {
            ui.small.header { +"None" }
        }

        is BackgroundJobRetryPolicyModel.LinearDelay -> {
            ui.small.header { +"Linear delay" }
            ui.content {
                +"Delay: ${policy.delayInMs}ms"
            }
            ui.content {
                +"Max tries: ${policy.maxTries}"
            }
        }
    }
}

internal fun FlowContent.renderJobData(data: String, dataHash: Int) {
    ui.meta { +"Hash: $dataHash" }

    pre {
        css {
            padding = Padding(5.px)
            backgroundColor = Color("#F0F0F0")
            overflow = Overflow.auto
        }
        +data
    }
}

internal fun FlowContent.renderResultsAsSegments(results: List<BackgroundJobResultModel>) {
    results.forEach { result ->
        ui.given(result.isFailure()) { red }
            .given(result.isSuccess()) { green }
            .segment {
                ui.divided.horizontal.list {
                    noui.item {
                        noui.content {
                            renderIcon(result) { big }
                        }
                    }
                    noui.item {
                        noui.header { +"Executed At" }
                        noui.content { +result.executedAt.atSystemDefaultZone().formatDdMmmYyyyHhMmSs() }
                    }
                    noui.item {
                        noui.header { +"Execution Time" }
                        noui.content { +"${result.executionTimeMs}ms" }
                    }
                    noui.item {
                        noui.header { +"Executed By" }
                        noui.content { +result.serverId }
                    }
                }

                ui.header { +"Result" }

                pre {
                    css {
                        padding = Padding(5.px)
                        backgroundColor = Color("#F0F0F0")
                        overflow = Overflow.auto
                    }
                    +result.data
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                }
            }
    }
}
