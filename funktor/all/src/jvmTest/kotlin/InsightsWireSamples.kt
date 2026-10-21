package io.peekandpoke.funktor

import java.io.File

/**
 * Writes [content] to `$INSIGHTS_WIRE_SAMPLES_DIR/<name>` when that variable is set; does nothing otherwise.
 *
 * The insights specs dump their real records and responses through this, so the wire contract in
 * `docs/tasks/20260824-insights-graphs-and-static-slices.md` can be re-read from current data rather
 * than inferred from the Kotlin types.
 */
fun insightsWireSample(name: String, content: String) {
    System.getenv("INSIGHTS_WIRE_SAMPLES_DIR")?.let { File(it, name).writeText(content) }
}
