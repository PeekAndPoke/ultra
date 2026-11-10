package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.kraft.addons.prismjs.Prism
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.code
import kotlinx.html.details
import kotlinx.html.div
import kotlinx.html.pre
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.summary
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.th
import kotlinx.html.thead
import kotlinx.html.title
import kotlinx.html.tr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * The insights pages' building blocks (ULTRA-00198).
 *
 * **Every string goes in as text** (`+text`), which preact escapes: there is no `unsafe { }` in these pages, the
 * kotlinx.html equivalent of `v-html`. Code goes through the Prism addon, which puts the text in as a text node and
 * lets Prism, which escapes, highlight it in place.
 */

/**
 * Longer text is not highlighted. Prism's greedy patterns rescan from each token, so text built to be many
 * unterminated comments costs quadratic time on the main thread; a huge plan reads fine as plain text.
 */
internal const val MAX_HIGHLIGHT_CHARS = 32_000

/** The page's own styles: wrapping code blocks, and the clickable rows. No recorded value is ever in here. */
internal const val INSIGHTS_CSS = """
.funktor-insights .insights-wrap pre { white-space: pre-wrap; word-break: break-word; }
.funktor-insights pre { max-height: 600px; overflow: auto; }
.funktor-insights .insights-records tbody tr { cursor: pointer; }
.funktor-insights .insights-mono { font-family: monospace; }
.funktor-insights .insights-path { word-break: break-all; }
"""

private val prettyJson = Json { prettyPrint = true }

/** A value as indented JSON. */
internal fun JsonElement.toPrettyText(): String = prettyJson.encodeToString(JsonElement.serializer(), this)

internal fun Tone.color(): String? = when (this) {
    Tone.Ok -> "green"
    Tone.Warn -> "yellow"
    Tone.Error -> "red"
    Tone.Neutral -> null
}

/** A label in [tone]'s colour. */
internal fun FlowContent.toneLabel(tone: Tone, text: String) {
    when (tone.color()) {
        "green" -> ui.green.label { +text }
        "yellow" -> ui.yellow.label { +text }
        "red" -> ui.red.label { +text }
        else -> ui.basic.label { +text }
    }
}

/** Key and value rows; a missing value reads n/a. */
internal fun FlowContent.factList(facts: List<Fact>) {
    ui.very.basic.compact.definition.table Table {
        tbody {
            facts.forEach { fact ->
                tr {
                    td { +fact.key }
                    td(classes = "insights-mono") {
                        val value = fact.value ?: "n/a"
                        if (fact.tone == Tone.Neutral) +value else toneLabel(fact.tone, value)
                    }
                }
            }
        }
    }
}

/** A strip of labelled figures, the old GUI's statistics. */
internal fun FlowContent.statStrip(cells: List<StatCell>) {
    ui.mini.statistics {
        cells.forEach { cell ->
            ui.statistic {
                cell.hint?.let { title = it }
                noui.value { +(cell.value ?: "n/a") }
                noui.label {
                    +cell.label
                    cell.hint?.let { hint -> div { ui.small.grey.text { +hint } } }
                }
            }
        }
    }
}

/** A map of names to lists, one row per value. */
internal fun FlowContent.keyValueTable(entries: Map<String, List<String>>, keyLabel: String, emptyText: String) {
    if (entries.isEmpty()) {
        ui.basic.segment { +emptyText }
        return
    }

    ui.very.compact.celled.table Table {
        thead { tr { th { +keyLabel }; th { +"Value" } } }
        tbody {
            entries.forEach { (key, values) ->
                values.ifEmpty { listOf("") }.forEach { value ->
                    tr {
                        td(classes = "insights-mono") { +key }
                        td(classes = "insights-mono insights-path") { +value }
                    }
                }
            }
        }
    }
}

/**
 * Code: a query, its values, a plan. Highlighted for `aql` and `json` up to [MAX_HIGHLIGHT_CHARS], plain text
 * otherwise. [wrap] wraps long lines.
 */
internal fun FlowContent.codeBlock(text: String?, language: String?, emptyText: String = "Nothing recorded", wrap: Boolean = false) {
    if (text.isNullOrEmpty()) {
        ui.basic.segment { +emptyText }
        return
    }

    div(classes = if (wrap) "insights-code insights-wrap" else "insights-code") {
        if (language != null && text.length <= MAX_HIGHLIGHT_CHARS) {
            Prism(language = language, code = text, options = {})
        } else {
            pre { code { +text } }
        }
    }
}

/** A JSON value, indented and highlighted: the raw view of a slice. */
internal fun FlowContent.jsonView(value: JsonElement?) {
    when (value) {
        null -> ui.basic.segment { +"Nothing recorded" }
        else -> codeBlock(text = value.toPrettyText(), language = "json")
    }
}

/** A collapsed section, opened by its [title]. */
internal fun FlowContent.collapsed(title: String, open: Boolean = false, body: FlowContent.() -> Unit) {
    details {
        if (open) attributes["open"] = ""
        summary { ui.small.header { +title } }
        div { body() }
    }
}

/** The raw slice, collapsed: the fallback of every readable tab. */
internal fun FlowContent.rawSlice(data: JsonElement?, title: String = "Raw slice") {
    collapsed(title) { jsonView(data) }
}

/** A short note under a section. */
internal fun FlowContent.note(text: String) {
    ui.small.grey.text P { +text }
}

/** An informational box. */
internal fun FlowContent.notice(text: String) {
    ui.info.message { +text }
}

/** An error box. */
internal fun FlowContent.problem(text: String) {
    ui.error.message { +text }
}

/** The page's style element; CSS only, constant. */
internal fun FlowContent.insightsStyles() {
    style { +INSIGHTS_CSS }
}

/** A cell that is muted when [text] is a placeholder. */
internal fun FlowContent.muted(text: String) {
    span { ui.grey.text { +text } }
}

