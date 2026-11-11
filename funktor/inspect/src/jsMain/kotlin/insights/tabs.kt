package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.code
import kotlinx.html.div
import kotlinx.serialization.json.JsonElement

/**
 * The detail page's tabs, by collector key (ULTRA-00198), as in the Vue GUI.
 *
 * The registry is OPEN: a key with no tab here (an app-defined collector) renders its raw JSON. A slice a tab
 * cannot read renders its raw JSON too, except the database slice, see [VaultTab].
 */
internal class InsightsTab(
    val label: String,
    val icon: FlowContent.() -> Unit,
    val render: FlowContent.(data: JsonElement, ui: InsightsUi) -> Unit,
)

/** Collector key to tab, with the old GUI's icons. */
internal val insightsTabs: Map<String, InsightsTab> = mapOf(
    "request" to InsightsTab("Request", { icon.cloud_upload_alternate() }) { data, _ -> requestTab(data) },
    "response" to InsightsTab("Response", { icon.cloud_download_alternate() }) { data, _ -> responseTab(data) },
    "user" to InsightsTab("User", { icon.user() }) { data, _ -> userTab(data) },
    "routing" to InsightsTab("Routing", { icon.compass_outline() }) { data, _ -> routingTab(data) },
    "runtime" to InsightsTab("Runtime", { icon.microchip() }) { data, _ -> runtimeTab(data) },
    "log" to InsightsTab("Logs", { icon.list() }) { data, _ -> logTab(data) },
    "vault" to InsightsTab("Database", { icon.database() }) { data, ui -> VaultTab(data, ui) },
    "kontainer" to InsightsTab("Kontainer", { icon.cubes() }) { data, ui -> KontainerTab(data, ui) },
    "app-config" to InsightsTab("Config", { icon.cog() }) { data, _ -> appConfigTab(data) },
)

private fun FlowContent.section(title: String? = null, body: FlowContent.() -> Unit) {
    div {
        title?.let { ui.dividing.header H4 { +it } }
        body()
    }
    ui.hidden.divider()
}

private fun FlowContent.requestTab(data: JsonElement) {
    val slice = readRequest(data) ?: return jsonView(data)

    section {
        factList(listOf(Fact("Method", slice.method), Fact("URL", slice.composeUrl(), full = true)))
        note("The recorded URL is a path; the query string is listed separately and is not part of it.")
    }
    section("Headers") { keyValueTable(slice.headers, "Header", "No headers recorded") }
    section("Query parameters") { keyValueTable(slice.queryParams, "Parameter", "No query parameters") }
    rawSlice(data)
}

private fun FlowContent.responseTab(data: JsonElement) {
    val slice = readResponse(data) ?: return jsonView(data)

    val status = slice.status?.let { code -> slice.statusDescription?.let { "$code $it" } ?: "$code" }

    section { factList(listOf(Fact("Status", status, toneForStatus(slice.status)))) }
    section("Headers") { keyValueTable(slice.headers, "Header", "No headers recorded") }
    rawSlice(data)
}

private fun FlowContent.userTab(data: JsonElement) {
    val slice = readUser(data) ?: return jsonView(data)

    section("User") {
        factList(
            listOf(
                Fact("User id", slice.userId),
                Fact("Description", slice.desc),
                Fact("Email", slice.email),
                Fact("Type", slice.type),
                Fact("Client IP", slice.clientIp),
                Fact("Kind", slice.kind),
                Fact(
                    "Superuser",
                    slice.isSuperUser?.let { if (it) "yes" else "no" },
                    if (slice.isSuperUser == true) Tone.Warn else Tone.Neutral,
                ),
            )
        )
    }
    section("Permissions") {
        note(
            "What this request resolved to. The server re-derives permissions from the verified token on every " +
                "request; nothing is authorized from a record."
        )
        jsonView(slice.permissions)
    }
    rawSlice(data)
}

private fun FlowContent.routingTab(data: JsonElement) {
    val trace = readRoutingTrace(data) ?: return jsonView(data)

    codeBlock(text = trace, language = null, emptyText = "No routing trace recorded")
}

private fun FlowContent.runtimeTab(data: JsonElement) {
    val slice = readRuntime(data) ?: return jsonView(data)

    section {
        statStrip(slice.cells())
        if (slice.fileDescriptorsUnmeasured) {
            note("File descriptor counts are 0 on non-Unix platforms -- that is \"not measured\", not \"none open\".")
        }
    }
    section("System properties") {
        keyValueTable(
            slice.systemProperties.mapValues { listOf(it.value) },
            "Property",
            "No system properties recorded",
        )
    }
    rawSlice(data)
}

private fun FlowContent.logTab(data: JsonElement) {
    val entries = readLogEntries(data) ?: return jsonView(data)

    if (entries.isEmpty()) {
        ui.basic.segment { +"No log entries" }
        return
    }

    // Two identical lines are ordinary: a retry loop logs the same message again, and that is often the finding
    entries.forEach { entry ->
        div(classes = "insights-log-entry") {
            toneLabel(toneForLogLevel(entry.level), entry.level ?: "UNKNOWN")
            codeBlock(text = entry.text, language = null, emptyText = "(empty log entry)", wrap = true)
        }
    }
}

private fun FlowContent.appConfigTab(data: JsonElement) {
    val slice = readAppConfig(data) ?: return jsonView(data)

    ui.info.message {
        +"Values declared as "
        code { +"Redacted" }
        +" appear as "
        code { +Redacted.PLACEHOLDER }
        +". Anything else is shown verbatim -- a secret held in a plain "
        code { +"String" }
        +" is recorded and displayed."
    }

    section("App info") { jsonView(slice.info) }
    section("Config") { jsonView(slice.config) }
}
