package io.peekandpoke.kraft.addons.chartjs

/** Utility helpers for Chart.js initialization and label generation. */
object ChartJsUtils {

    private var alreadyRegistered = false

    /** Registers all Chart.js modules (controllers, scales, elements, plugins). Called once. */
    fun registerAllModules() {
        if (!alreadyRegistered) {
            alreadyRegistered = true

//            console.info("Chart.register", Chart.Companion::register)
//            console.info("registerables", registerables)

            Chart.register(*registerables)
        }
    }

    /** Creates numeric labels from 1 to [count]. */
    fun numberLabels(count: Int): Array<String> = numberLabels(1..count)

    /** Creates numeric labels for the given [range]. */
    fun numberLabels(range: IntRange) = range.map { it.toString() }.toTypedArray()
}
