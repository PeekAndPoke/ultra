package io.peekandpoke.funktor.insights

/**
 * One collector's slice of an insights record.
 *
 * Implementations are plain DTOs: they carry data and nothing else. Rendering lives in the frontend,
 * which reaches this through the insights API and looks the slice up by [key].
 *
 * VUE-REF: the original kotlinx.html rendering hooks — `renderBar` / `renderDetails` and the
 * `menu` / `content` / `inlineScript` helpers — are preserved at
 * `funktor/insights/reference/InsightsCollectorData.kt`.
 */
interface InsightsCollectorData {
    /**
     * Stable identifier for this slice, e.g. `"request"`.
     *
     * **Declared, never derived from the class.** The frontend addresses tabs by this string, so
     * deriving it from a qualified class name — as `templateKey` used to — would silently orphan a tab
     * on any rename or package move.
     */
    val key: String
}
