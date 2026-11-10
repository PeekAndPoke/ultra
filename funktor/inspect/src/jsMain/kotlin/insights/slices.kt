package io.peekandpoke.funktor.inspect.insights

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Readers for the collector slices: the Kotlin twin of the Vue GUI's `slices.ts` (ULTRA-00198).
 *
 * A slice is a [JsonElement] on purpose (the envelope names no collector), so these are narrowing readers, not
 * decoders. Records outlive the code that wrote them: a reader returns null for what it cannot read, and the tab
 * falls back to the raw JSON. Everything read here is attacker-controlled; rendering escapes, nothing sanitises.
 */

//  Primitive narrowing  ///////////////////////////////////////////////////////////////////////////////////////////////

internal fun JsonElement?.asRecord(): JsonObject? = this as? JsonObject

internal fun JsonElement?.asArray(): JsonArray? = this as? JsonArray

internal fun JsonElement?.asString(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun JsonElement?.asNumber(): Double? =
    (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull?.takeIf { !it.isNaN() }

internal fun JsonElement?.asBoolean(): Boolean? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull

/** ktor's `HttpMethod` and `HttpStatusCode` serialise as `{value, ...}`; a bare scalar is accepted too. */
internal fun JsonElement?.asWrappedString(): String? = asString() ?: asRecord()?.get("value").asString()

internal fun JsonElement?.asWrappedNumber(): Double? = asNumber() ?: asRecord()?.get("value").asNumber()

private val compact = Json

/** Indented JSON, for display. */
internal val prettyJson = Json { prettyPrint = true }

/** A JSON value as compact text, for a cell that holds something other than a string. */
internal fun JsonElement.toCompactText(): String = compact.encodeToString(JsonElement.serializer(), this)

/** A header or query map, `{"Name": ["v1", "v2"]}`. A bare value is lifted into a one-element list, never dropped. */
internal fun JsonElement?.asStringListMap(): Map<String, List<String>>? {
    val record = asRecord() ?: return null

    return record.mapValues { (_, raw) ->
        raw.asArray()?.map { it.asString() ?: it.toCompactText() }
            ?: listOf(raw.asString() ?: raw.toCompactText())
    }
}

/** A whole number without a fraction, anything else as it is. */
internal fun Double.formatNumber(): String = if (this == floor(this) && !isInfinite()) {
    toLong().toString()
} else {
    toString()
}

internal fun Double.toFixed(digits: Int): String = asDynamic().toFixed(digits).unsafeCast<String>()

/** Nanoseconds as milliseconds, at the precision the old GUI used. */
internal fun nsToMs(ns: Double?): String? = ns?.let { "${(it / 1_000_000).toFixed(2)} ms" }

/** Bytes as whole megabytes, the unit the old tab used. */
internal fun bytesToMb(bytes: Double?): String? = bytes?.let { "${(it / (1024 * 1024)).roundToLong()} MB" }

/** The last segment of a class name. */
internal fun shortName(fqn: String?): String = fqn?.substringAfterLast('.') ?: "n/a"

//  request  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

internal data class RequestSlice(
    val method: String?,
    val scheme: String?,
    val host: String?,
    val port: Double?,
    /** Path only: scheme, host and query string are not in it. */
    val uri: String?,
    val headers: Map<String, List<String>>,
    val queryParams: Map<String, List<String>>,
) {
    /** The full URL; the default port is left in, since "which port did it arrive on" is a debugging question. */
    fun composeUrl(): String? {
        val path = uri ?: return null
        if (scheme == null || host == null) return path

        return "$scheme://$host${port?.let { ":${it.formatNumber()}" } ?: ""}$path"
    }
}

internal fun readRequest(data: JsonElement?): RequestSlice? {
    val record = data.asRecord() ?: return null

    return RequestSlice(
        method = record["method"].asWrappedString(),
        scheme = record["scheme"].asString(),
        host = record["host"].asString(),
        port = record["port"].asNumber(),
        uri = record["uri"].asString(),
        headers = record["headers"].asStringListMap() ?: emptyMap(),
        queryParams = record["queryParams"].asStringListMap() ?: emptyMap(),
    )
}

//  response  //////////////////////////////////////////////////////////////////////////////////////////////////////////

internal data class ResponseSlice(
    val status: Int?,
    val statusDescription: String?,
    val headers: Map<String, List<String>>,
)

internal fun readResponse(data: JsonElement?): ResponseSlice? {
    val record = data.asRecord() ?: return null

    return ResponseSlice(
        status = record["status"].asWrappedNumber()?.toInt(),
        statusDescription = record["status"].asRecord()?.get("description").asString(),
        headers = record["headers"].asStringListMap() ?: emptyMap(),
    )
}

//  user  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

internal data class UserSlice(
    val userId: String?,
    val clientIp: String?,
    val email: String?,
    val desc: String?,
    val type: String?,
    /** The polymorphic discriminator `_type`: `anonymous`, `system`, `logged-in`, `api-key`. */
    val kind: String?,
    val isSuperUser: Boolean?,
    /** Left raw: the permission sets are the app's shape. */
    val permissions: JsonElement?,
)

internal fun readUser(data: JsonElement?): UserSlice? {
    val record = data.asRecord() ?: return null
    val user = record["user"].asRecord()
    val permissions = record["permissions"].asRecord()

    return UserSlice(
        userId = user?.get("userId").asString(),
        clientIp = user?.get("clientIp").asString(),
        email = user?.get("email").asString(),
        desc = user?.get("desc").asString(),
        type = user?.get("type").asString(),
        kind = user?.get("_type").asString(),
        isSuperUser = permissions?.get("isSuperUser").asBoolean(),
        permissions = record["permissions"],
    )
}

//  routing, template, log  ////////////////////////////////////////////////////////////////////////////////////////////

internal fun readRoutingTrace(data: JsonElement?): String? = data.asRecord()?.get("trace").asString()

/** Null only when unrecognisable; a null [timeNs] is the NORMAL case, every request that rendered no view. */
internal data class TemplateSlice(val timeNs: Double?)

internal fun readTemplate(data: JsonElement?): TemplateSlice? {
    val record = data.asRecord() ?: return null
    if (!record.containsKey("timeNs")) return null

    return TemplateSlice(timeNs = record["timeNs"].asNumber())
}

/** One log line; [text] is already formatted by the appender. */
internal data class LogEntry(val level: String?, val text: String?)

internal fun readLogEntries(data: JsonElement?): List<LogEntry>? {
    val entries = data.asRecord()?.get("entries").asArray() ?: return null

    return entries.map {
        val record = it.asRecord()
        LogEntry(level = record?.get("level").asString(), text = record?.get("text").asString())
    }
}

//  runtime  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

internal data class RuntimeSlice(
    val jvmVersion: String?,
    val kotlinVersion: String?,
    val cpus: Double?,
    val maxMem: Double?,
    val reservedMem: Double?,
    val freeMem: Double?,
    val openFileDescriptors: Double?,
    val maxFileDescriptors: Double?,
    val systemProperties: Map<String, String>,
) {
    /** Both counts are 0 on a non-Unix JVM: not measured, not none open. */
    val fileDescriptorsUnmeasured: Boolean get() = openFileDescriptors == 0.0 && maxFileDescriptors == 0.0
}

internal fun readRuntime(data: JsonElement?): RuntimeSlice? {
    val record = data.asRecord() ?: return null

    return RuntimeSlice(
        jvmVersion = record["jvmVersion"].asString(),
        kotlinVersion = record["kotlinVersion"].asString(),
        cpus = record["cpus"].asNumber(),
        maxMem = record["maxMem"].asNumber(),
        reservedMem = record["reservedMem"].asNumber(),
        freeMem = record["freeMem"].asNumber(),
        openFileDescriptors = record["openFileDescriptors"].asNumber(),
        maxFileDescriptors = record["maxFileDescriptors"].asNumber(),
        systemProperties = record["systemProperties"].asRecord()
            ?.mapValues { (_, value) -> value.asString() ?: value.toCompactText() }
            ?: emptyMap(),
    )
}

//  vault  /////////////////////////////////////////////////////////////////////////////////////////////////////////////

internal data class VaultMeasure(val totalNs: Double, val count: Double) {
    operator fun plus(other: VaultMeasure) = VaultMeasure(totalNs + other.totalNs, count + other.count)

    /** `X ms (Nx)`: the old per-query format, where the count matters as much as the time. */
    fun format(): String = "${nsToMs(totalNs)} (${count.formatNumber()}x)"

    companion object {
        val zero = VaultMeasure(0.0, 0.0)
    }
}

internal data class VaultEntry(
    val connection: String?,
    val count: Double?,
    val totalCount: Double?,
    /** The query text; null when the server withheld it. Decided by the backend (ULTRA-00171), not here. */
    val query: String?,
    val queryLanguage: String?,
    /** Null when unknown: Monko's old `json` entries counted 0 whatever they had. */
    val varsCount: Double?,
    /** Each bind value as JSON text, by name; null when withheld or not recorded. */
    val values: Map<String, String>?,
    /** The repository holds credentials: neither values nor plan were recorded, only their count. */
    val valuesWithheld: Boolean,
    val plan: String?,
    /** The slice predates `VaultCollector.Data`: its text may hold inlined values, so nothing of it is shown. */
    val legacy: Boolean,
    val totalNs: Double,
    val serializer: VaultMeasure,
    val query_: VaultMeasure,
    val iterator: VaultMeasure,
    val deserializer: VaultMeasure,
    val explain: VaultMeasure,
)

internal data class VaultSlice(
    val entries: List<VaultEntry>,
    /** Summed here: the per-slice totals are not in the record. */
    val totalNs: Double,
    val serializer: VaultMeasure,
    val query: VaultMeasure,
    val iterator: VaultMeasure,
    val deserializer: VaultMeasure,
    val explain: VaultMeasure,
)

private fun JsonElement?.readMeasure(): VaultMeasure {
    val record = asRecord()

    return VaultMeasure(
        totalNs = record?.get("totalNs").asNumber() ?: 0.0,
        count = record?.get("count").asNumber() ?: 0.0,
    )
}

private fun List<VaultMeasure>.sum(): VaultMeasure = fold(VaultMeasure.zero) { acc, it -> acc + it }

internal fun readVault(data: JsonElement?): VaultSlice? {
    val rawEntries = data.asRecord()?.get("entries").asArray() ?: return null

    val entries = rawEntries.map { raw ->
        val record = raw.asRecord() ?: JsonObject(emptyMap())
        // The collector never writes `vars` or `queryExplained`; either OLD key marks the entry legacy
        val legacy = record.containsKey("vars") || record.containsKey("queryExplained")
        val language = record["queryLanguage"].asString()

        VaultEntry(
            connection = record["connection"].asString(),
            count = record["count"].asNumber(),
            totalCount = record["totalCount"].asNumber(),
            query = if (legacy) null else record["query"].asString(),
            queryLanguage = language,
            varsCount = when {
                language == "json" -> null
                else -> record["varsCount"].asNumber()
                    ?: if (legacy) (record["vars"].asRecord()?.size ?: 0).toDouble() else 0.0
            },
            values = if (legacy) {
                null
            } else {
                record["values"].asRecord()?.mapNotNull { (name, text) -> text.asString()?.let { name to it } }?.toMap()
            },
            valuesWithheld = record["valuesWithheld"].asBoolean() == true,
            plan = if (legacy) null else record["explain"].asString(),
            legacy = legacy,
            totalNs = record["totalNs"].asNumber() ?: 0.0,
            serializer = record["measureSerializer"].readMeasure(),
            query_ = record["measureQuery"].readMeasure(),
            iterator = record["measureIterator"].readMeasure(),
            deserializer = record["measureDeserializer"].readMeasure(),
            explain = record["measureExplain"].readMeasure(),
        )
    }

    return VaultSlice(
        entries = entries,
        totalNs = entries.sumOf { it.totalNs },
        serializer = entries.map { it.serializer }.sum(),
        query = entries.map { it.query_ }.sum(),
        iterator = entries.map { it.iterator }.sum(),
        deserializer = entries.map { it.deserializer }.sum(),
        explain = entries.map { it.explain }.sum(),
    )
}

/**
 * The bind values as one pretty JSON document, `{name: value}`.
 *
 * A value is pretty-printed only when re-printing it gives the server's text back exactly; otherwise it is shown as
 * the server wrote it (a value cut at the length limit, a number JavaScript cannot hold). Built per name, never by
 * substituting into the whole: a value can contain anything.
 */
internal fun valuesText(values: Map<String, String>): String {
    if (values.isEmpty()) return "{}"

    val lines = values.map { (name, text) ->
        "  ${JsonPrimitive(name).toCompactText()}: ${valueDisplay(text).replace("\n", "\n  ")}"
    }

    return "{\n${lines.joinToString(",\n")}\n}"
}

private fun valueDisplay(text: String): String = try {
    val value = compact.parseToJsonElement(text)
    if (value.toCompactText() == text) prettyJson.encodeToString(JsonElement.serializer(), value) else text
} catch (_: Throwable) {
    JsonPrimitive(text).toCompactText()
}

//  kontainer  /////////////////////////////////////////////////////////////////////////////////////////////////////////

internal data class KontainerInjection(
    val name: String?,
    /** The DECLARED parameter types; one may name no service at all. */
    val classes: List<String>,
    val provisionType: String?,
)

internal data class KontainerDefinition(
    val creates: String?,
    val injectionType: String?,
    val injects: List<KontainerInjection>,
    val codeLocation: String?,
    val overwrites: KontainerDefinition?,
) {
    /** The `overwrites` chain, flat; the first is the definition in force. */
    fun chain(): List<KontainerDefinition> = generateSequence(this) { it.overwrites }.toList()
}

internal data class KontainerInstance(
    val cls: String?,
    /** Epoch MILLIS, from either shape a record can carry. */
    val createdAtMillis: Double?,
)

internal data class KontainerService(
    val cls: String?,
    val type: String?,
    /** Null on a `slim` record, whose definitions live in the kontainer graph. */
    val definition: KontainerDefinition?,
    val instances: List<KontainerInstance>,
) {
    /** A Singleton is process state, shared by every request; every other type is this request's own. */
    val isProcessWide: Boolean get() = type == "Singleton"
}

internal enum class KontainerShape {
    /** Written before 2026-10-19: every service, definitions inline. */
    Full,

    /** Since: only services with instances, joined to the kontainer graph by `cls`. */
    Slim,
}

internal data class KontainerSlice(
    val shape: KontainerShape,
    val numOld: Double?,
    val numTotal: Double?,
    val graphFingerprint: String?,
    val services: List<KontainerService>,
)

/** The kontainer graph endpoint's data, in the reader's shape. */
internal data class KontainerGraphData(
    val fingerprint: String?,
    val services: List<KontainerService>,
)

/** `{"fqn": "..."}` on a `full` record, a flat string on a `slim` one and in the graph. */
private fun JsonElement?.readFqn(): String? = asRecord()?.get("fqn").asString() ?: asString()

/** An instant as epoch millis: Slumber writes `{ts, ...}`, the Jackson-era writer epoch SECONDS as a double. */
internal fun JsonElement?.readInstantMillis(): Double? =
    asRecord()?.get("ts").asNumber() ?: asNumber()?.let { it * 1000 }

private fun JsonElement?.readDefinition(depth: Int = 0): KontainerDefinition? {
    val record = asRecord() ?: return null

    return KontainerDefinition(
        creates = record["creates"].readFqn(),
        injectionType = record["injectionType"].asString(),
        injects = record["injects"].asArray()?.map { raw ->
            val inject = raw.asRecord()
            KontainerInjection(
                name = inject?.get("name").asString(),
                classes = inject?.get("classes").asArray()?.mapNotNull { it.readFqn() } ?: emptyList(),
                provisionType = inject?.get("provisionType").asString(),
            )
        } ?: emptyList(),
        codeLocation = record["codeLocation"].asString() ?: record["codeLocation"].asRecord()?.get("location").asString(),
        // A chain; bounded, since a cycle in a malformed record would hang the tab
        overwrites = if (depth < 16) record["overwrites"].readDefinition(depth + 1) else null,
    )
}

private fun JsonElement?.readService(): KontainerService {
    val service = asRecord()

    return KontainerService(
        cls = service?.get("cls").readFqn(),
        type = service?.get("type").asString(),
        definition = service?.get("definition").readDefinition(),
        instances = service?.get("instances").asArray()?.map { raw ->
            val instance = raw.asRecord()
            KontainerInstance(
                cls = instance?.get("cls").readFqn(),
                createdAtMillis = instance?.get("createdAt").readInstantMillis(),
            )
        } ?: emptyList(),
    )
}

/** The old table's order: services with instances first, then by FQN. */
internal fun List<KontainerService>.sortedForTable(): List<KontainerService> =
    sortedWith(compareBy<KontainerService> { it.instances.isEmpty() }.thenBy { it.cls ?: "" })

/** Either shape, told apart by what is there: the `graphFingerprint` key marks `slim`, `info` marks `full`. */
internal fun readKontainer(data: JsonElement?): KontainerSlice? {
    val record = data.asRecord() ?: return null
    val slim = record.containsKey("graphFingerprint")
    val rawServices = if (slim) record["services"].asArray() else record["info"].asRecord()?.get("services").asArray()

    rawServices ?: return null

    return KontainerSlice(
        shape = if (slim) KontainerShape.Slim else KontainerShape.Full,
        numOld = if (slim) null else record["numOld"].asNumber(),
        numTotal = if (slim) null else record["numTotal"].asNumber(),
        graphFingerprint = record["graphFingerprint"].asString(),
        services = rawServices.map { it.readService() }.sortedForTable(),
    )
}

/**
 * A `slim` record's services with their definitions from [graph], joined on `cls`; with [includeAll], the graph's
 * services without instances too. The type stays the record's: only the record can say `DynamicOverride`, and such
 * a service gets [overrideDefinition] rather than the blueprint's, whose injects never ran for it.
 */
internal fun KontainerSlice.joinGraph(graph: KontainerGraphData, includeAll: Boolean): List<KontainerService> {
    val byCls = graph.services.filter { it.cls != null }.associateBy { it.cls }

    val joined = services.map { service ->
        val blueprint = service.cls?.let { byCls[it]?.definition }
        val definition = service.definition
            ?: if (service.type == "DynamicOverride") overrideDefinition(service, blueprint) else blueprint

        service.copy(definition = definition)
    }

    if (!includeAll) return joined

    val recorded = services.map { it.cls }.toSet()
    val rest = graph.services.filter { it.cls !in recorded }.map { it.copy(instances = emptyList()) }

    return (joined + rest).sortedForTable()
}

/** What a `full` record wrote for a `DynamicOverride`: its own class, `Dynamic`, no injects, overwriting the blueprint. */
internal fun overrideDefinition(service: KontainerService, blueprint: KontainerDefinition?) = KontainerDefinition(
    creates = service.instances.firstOrNull()?.cls ?: service.cls,
    injectionType = "Dynamic",
    injects = emptyList(),
    codeLocation = null,
    overwrites = blueprint,
)

internal enum class GraphMatch { Same, Other, Unknown }

/** Whether a `slim` record was written against [graph]; a `full` record carries its own. */
internal fun KontainerSlice.matches(graph: KontainerGraphData): GraphMatch = when {
    shape == KontainerShape.Full -> GraphMatch.Same
    graphFingerprint == null -> GraphMatch.Unknown
    graphFingerprint == graph.fingerprint -> GraphMatch.Same
    else -> GraphMatch.Other
}

//  app-config and build info  /////////////////////////////////////////////////////////////////////////////////////////

internal data class AppConfigSlice(val info: JsonElement?, val config: JsonElement?)

internal fun readAppConfig(data: JsonElement?): AppConfigSlice? {
    val record = data.asRecord() ?: return null
    if (!record.containsKey("info") && !record.containsKey("config")) return null

    return AppConfigSlice(info = record["info"], config = record["config"])
}

/** The git version, application id and environment the old insights bar showed (ULTRA-00190). */
internal data class BuildInfo(
    val git: String?,
    val gitDesc: String?,
    val applicationId: String?,
    val environment: String?,
) {
    /** The facts both pages show; the git description goes next to the version. */
    fun facts(): List<Fact> = listOf(
        Fact("Git", git?.let { if (gitDesc == null) it else "$it ($gitDesc)" }),
        Fact("Application", applicationId),
        Fact("Environment", environment),
    )
}

/** `AppVersion`'s placeholder for a build without git info: not a version, so shown as missing. */
private fun String?.known(): String? = this?.takeUnless { it.isEmpty() || it == "n/a" }

/** The record's own build info, from its `app-config` slice; null without one. */
internal fun readBuildInfo(data: JsonElement?): BuildInfo? {
    val slice = readAppConfig(data) ?: return null
    val version = slice.info.asRecord()?.get("version").asRecord()
    val ktor = slice.config.asRecord()?.get("ktor").asRecord()
    val git = listOfNotNull(version?.get("gitBranch").asString().known(), version?.get("gitRev").asString().known())
        .joinToString("-")

    return BuildInfo(
        git = git.ifEmpty { null },
        gitDesc = version?.get("gitDesc").asString().known(),
        applicationId = ktor?.get("application").asRecord()?.get("id").asString().known(),
        environment = ktor?.get("deployment").asRecord()?.get("environment").asString().known(),
    )
}

internal fun serverBuildInfo(git: String, gitDesc: String, applicationId: String, environment: String) = BuildInfo(
    git = git.known(),
    gitDesc = gitDesc.known(),
    applicationId = applicationId.known(),
    environment = environment.known(),
)

//  Facts and stat cells  //////////////////////////////////////////////////////////////////////////////////////////////

/** One row of a fact list; a null [value] reads n/a. */
internal data class Fact(val key: String, val value: String?, val tone: Tone = Tone.Neutral)

/** One cell of a stat strip. */
internal data class StatCell(val label: String, val value: String?, val hint: String? = null)

internal enum class Tone { Ok, Warn, Error, Neutral }

/** The shape the old GUI applied to response time, render time and container age. */
internal fun toneAbove(value: Double?, warn: Double, error: Double): Tone = when {
    value == null || value.isNaN() -> Tone.Neutral
    value > error -> Tone.Error
    value > warn -> Tone.Warn
    else -> Tone.Ok
}

internal fun toneForStatus(status: Int?): Tone = when {
    status == null -> Tone.Neutral
    status in 200..299 -> Tone.Ok
    status in 400..499 -> Tone.Warn
    status >= 500 -> Tone.Error
    else -> Tone.Neutral
}

/** `OFF` with `ERROR`, as the old GUI did: `OFF` reaching an appender means something logged above every threshold. */
internal fun toneForLogLevel(level: String?): Tone = when (level) {
    "INFO" -> Tone.Ok
    "WARNING" -> Tone.Warn
    "ERROR", "OFF" -> Tone.Error
    else -> Tone.Neutral
}

/** The seven runtime cells, in the tab and in the Overview. */
internal fun RuntimeSlice.cells(): List<StatCell> = listOf(
    StatCell("JVM", jvmVersion),
    StatCell("Kotlin", kotlinVersion),
    StatCell("CPUs", cpus?.formatNumber()),
    StatCell("Free heap", bytesToMb(freeMem)),
    StatCell("Reserved heap", bytesToMb(reservedMem)),
    StatCell("Max heap", bytesToMb(maxMem)),
    StatCell(
        "File descriptors",
        openFileDescriptors?.formatNumber(),
        hint = if (fileDescriptorsUnmeasured) {
            "not measured on this platform"
        } else {
            "max: ${maxFileDescriptors?.formatNumber() ?: "n/a"}"
        },
    ),
)

/** The seven database cells, in the tab and in the Overview. */
internal fun VaultSlice.cells(): List<StatCell> = listOf(
    StatCell("Queries", entries.size.toString()),
    StatCell("Total", nsToMs(totalNs)),
    StatCell("Serializer", nsToMs(serializer.totalNs)),
    StatCell("Query", nsToMs(query.totalNs)),
    StatCell("Iterator", nsToMs(iterator.totalNs)),
    StatCell("Deserializer", nsToMs(deserializer.totalNs)),
    StatCell("Explain", nsToMs(explain.totalNs)),
)

