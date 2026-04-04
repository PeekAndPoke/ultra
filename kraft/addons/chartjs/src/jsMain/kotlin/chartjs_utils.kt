package io.peekandpoke.kraft.addons.chartjs

/** Utility helpers for Chart.js label generation. */
object ChartJsUtils {

    /** Creates numeric labels from 1 to [count]. */
    fun numberLabels(count: Int): Array<String> = numberLabels(1..count)

    /** Creates numeric labels for the given [range]. */
    fun numberLabels(range: IntRange) = range.map { it.toString() }.toTypedArray()
}
