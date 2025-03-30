package de.peekandpoke.kraft.addons.chartjs

external interface ChartConfig {

    interface ChartData {

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

    interface Options {
        var elements: dynamic
        var plugins: dynamic
        var scales: dynamic
    }

    var type: String

    var data: ChartData

    var options: Options
}
