package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.kraft.addons.prismjs.Prism
import io.peekandpoke.ultra.common.model.Redacted
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

/**
 * The page's own styles: wrapping code blocks, and the clickable rows. No recorded value is ever in here. The wrap
 * rule names `code` too: Prism's theme gives a highlighted `code` its own `white-space: pre`.
 */
internal const val INSIGHTS_CSS = """
.funktor-insights .insights-wrap pre,
.funktor-insights .insights-wrap pre code { white-space: pre-wrap; word-break: break-word; word-wrap: break-word; }
.funktor-insights pre { max-height: 600px; overflow: auto; }
.funktor-insights .insights-records tbody tr { cursor: pointer; }
.funktor-insights .insights-mono { font-family: monospace; }
.funktor-insights .insights-path { word-break: break-all; }
.funktor-insights .insights-redacted { color: grey; font-style: italic; }
.funktor-insights .insights-facts {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(13em, 1fr)); gap: 0.5em; margin: 0.5em 0 1em;
}
.funktor-insights .insights-facts:last-child { margin-bottom: 0; }
.funktor-insights .insights-fact {
  min-width: 0; padding: 0.45em 0.7em; border: 1px solid rgba(34, 36, 38, 0.15); border-radius: 0.28rem;
  background: #fff;
}
.funktor-insights .insights-fact.wide { grid-column: span 2; }
.funktor-insights .insights-fact.full { grid-column: 1 / -1; }
.funktor-insights .insights-fact-key {
  margin-bottom: 0.15em; font-size: 0.75em; letter-spacing: 0.03em; text-transform: uppercase;
  color: rgba(0, 0, 0, 0.5); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.funktor-insights .insights-fact-value { font-family: monospace; word-break: break-word; }
.funktor-insights .insights-toolbar { margin: 0 0 1em; }
.funktor-insights .ui.segment.insights-tab-body { padding: 0.75em; }
.funktor-insights .insights-stat-value { font-size: 1.35em; line-height: 1.3; }
.funktor-insights .insights-fact-hint { font-size: 0.8em; color: rgba(0, 0, 0, 0.5); }
.funktor-insights .insights-summary { margin: 1em 0 0.5em; font-weight: bold; cursor: pointer; }
"""

/** A value as indented JSON. */
internal fun JsonElement.toPrettyText(): String = prettyJson.encodeToString(JsonElement.serializer(), this)

/** A label in [tone]'s colour. */
internal fun FlowContent.toneLabel(tone: Tone, text: String) {
    when (tone) {
        Tone.Ok -> ui.green.label { +text }
        Tone.Warn -> ui.yellow.label { +text }
        Tone.Error -> ui.red.label { +text }
        Tone.Neutral -> ui.basic.label { +text }
    }
}

/**
 * Facts as a grid of small cards, a muted label above the value: fills the width and wraps on a narrow screen. A long
 * value (a git description) takes two columns, a [Fact.full] one the whole row.
 */
internal fun FlowContent.factList(facts: List<Fact>) {
    div(classes = "insights-facts") {
        facts.forEach { fact ->
            val value = fact.value ?: "n/a"

            val width = when {
                fact.full -> " full"
                value.length > WIDE_FACT_CHARS -> " wide"
                else -> ""
            }

            div(classes = "insights-fact$width") {
                div(classes = "insights-fact-key") { +fact.key }
                div(classes = "insights-fact-value") {
                    if (fact.tone == Tone.Neutral) +value else toneLabel(fact.tone, value)
                }
            }
        }
    }
}

/** From this many characters on, a fact's card takes two columns. */
private const val WIDE_FACT_CHARS = 28

/** Labelled figures as cards, like [factList]: the label above, the figure larger below; titles do not break. */
internal fun FlowContent.statStrip(cells: List<StatCell>) {
    div(classes = "insights-facts") {
        cells.forEach { cell ->
            div(classes = "insights-fact") {
                cell.hint?.let { title = it }

                div(classes = "insights-fact-key") { +cell.label }
                div(classes = "insights-stat-value") { +(cell.value ?: "n/a") }
                cell.hint?.let { hint -> div(classes = "insights-fact-hint") { +hint } }
            }
        }
    }
}

/**
 * A map of names to lists, sorted by name, one row per name and one line per value: a repeated header stays
 * visibly two values, not one with a comma. A value the server redacted (`Redacted.PLACEHOLDER`) is muted.
 */
internal fun FlowContent.keyValueTable(entries: Map<String, List<String>>, keyLabel: String, emptyText: String) {
    if (entries.isEmpty()) {
        ui.basic.segment { +emptyText }
        return
    }

    ui.very.compact.celled.table Table {
        thead { tr { th { +keyLabel }; th { +"Value" } } }
        tbody {
            entries.entries.sortedWith(compareBy(localeOrder) { it.key }).forEach { (key, values) ->
                tr {
                    // As narrow as the longest name: the values get the width
                    td(classes = "collapsing insights-mono") { +key }
                    td(classes = "insights-mono insights-path") {
                        values.forEach { value ->
                            div(classes = if (value == Redacted.PLACEHOLDER) "insights-redacted" else null) { +value }
                        }
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
internal fun FlowContent.collapsed(title: String, body: FlowContent.() -> Unit) {
    details {
        // The title inline in the summary, so the disclosure arrow and the title share one line
        summary(classes = "insights-summary") { +title }
        div { body() }
    }
}

/** The raw slice, collapsed: the fallback of every readable tab. */
internal fun FlowContent.rawSlice(data: JsonElement?, title: String = "Raw slice") {
    rawSliceText(data?.toPrettyText(), title)
}

/** [rawSlice] for text already encoded, by a tab that keeps it between renders. */
internal fun FlowContent.rawSliceText(text: String?, title: String = "Raw slice") {
    collapsed(title) { codeBlock(text = text, language = "json") }
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

/** Grey text, for placeholders and secondary detail. */
internal fun FlowContent.muted(text: String) {
    span { ui.grey.text { +text } }
}

