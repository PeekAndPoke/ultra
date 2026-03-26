package io.peekandpoke.kraft.addons.chartjs

/** External interface representing a complete Chart.js configuration (type, data, options). */
external interface ChartConfig {

    /** The data section of a Chart.js configuration, containing labels and datasets. */
    interface ChartData {

        /** A single dataset within a chart, including data points and styling. */
        interface Dataset {
            // Labeling
            var label: String

            // Axes
            var yAxisID: String

            // Data
            var data: Array<Number>

            // Styling - Color
            var fill: Boolean
            var color: StringOrArray
            var backgroundColor: StringOrArray
            var borderColor: StringOrArray
            var borderWidth: NumberOrArray

            // Styling
            var stepped: String // "left", "middle", "right"
        }

        var labels: Array<String>

        var datasets: Array<Dataset>
    }

    /** Chart-level options (elements, plugins, scales). */
    interface Options {
        var elements: dynamic
        var plugins: dynamic
        var scales: dynamic
    }

    var type: String

    var data: ChartData

    var options: Options
}
