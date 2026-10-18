package io.peekandpoke.kraft.addons.chartjs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.await
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.Element
import kotlin.js.Promise

// Host that re-renders ChartJs with whatever config the stream holds
@Suppress("TestFunctionName")
private fun Tag.ChartHost(source: StreamSource<ChartConfig>) = comp(
    ChartHost.Props(source = source)
) {
    ChartHost(it)
}

private class ChartHost(ctx: Ctx<Props>) : Component<ChartHost.Props>(ctx) {

    data class Props(
        val source: StreamSource<ChartConfig>,
    )

    private val config by subscribingTo(props.source)

    override fun VDom.render() {
        div {
            style = "width: 400px; height: 300px;"
            ChartJs(config)
        }
    }
}

/** The live Chart.js instance on the test bed's canvas, or undefined. */
private suspend fun KQuery<Element>.liveChart(): dynamic {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('chart.js')") as Promise<dynamic>).await()

    return module.Chart.getChart(selectCss("canvas").firstOrNull())
}

private fun dataset(
    label: String,
    values: Array<Number> = arrayOf(1, 2, 3),
    block: ChartConfig.ChartData.Dataset.() -> Unit = {},
) = jsObject<ChartConfig.ChartData.Dataset> {
    this.label = label
    data = values
    block()
}

private fun chartConfig(
    type: String = "line",
    labels: Array<String> = arrayOf("a", "b", "c"),
    datasets: Array<ChartConfig.ChartData.Dataset>,
    options: ChartConfig.Options = jsObject { asDynamic().animation = false },
) = jsObject<ChartConfig> {
    this.type = type
    data = jsObject {
        this.labels = labels
        this.datasets = datasets
    }
    this.options = options
}

/** Mounts a chart with [initial] and runs [test] once Chart.js has created it. */
private suspend fun withChart(
    initial: ChartConfig,
    test: suspend (source: StreamSource<ChartConfig>, root: KQuery<Element>) -> Unit,
) {
    val source = StreamSource(initial)

    TestBed.preact(
        appSetup = { addons { chartJs() } },
        view = { ChartHost(source) },
    ) { root ->
        eventually { (root.liveChart() != null) shouldBe true }

        test(source, root)
    }
}

/** [count] points `{x, y}` for a linear x-axis. */
private fun points(count: Int): Array<Number> =
    Array<Any>(count) { i -> jsObject<dynamic> { x = i; y = i * 2 }.unsafeCast<Any>() }
        .unsafeCast<Array<Number>>()

/**
 * Options that make Chart.js decimate a line dataset of more than 10 points down to 20.
 *
 * LTTB with fixed samples, not min-max: min-max keeps up to 4 points per pixel column, so on a 400px canvas 200
 * points would come out as about 200 and "decimated" could not be told from "full".
 */
private fun decimationOptions() = jsObject<ChartConfig.Options> {
    asDynamic().animation = false
    asDynamic().parsing = false
    scales = jsObject { x = jsObject { type = "linear" } }
    plugins = jsObject { decimation = jsObject { enabled = true; algorithm = "lttb"; samples = 20; threshold = 10 } }
}

private fun labelsOf(chart: dynamic): List<String> =
    chart.data.datasets.unsafeCast<Array<dynamic>>().map { it.label.unsafeCast<String>() }

class ChartJsComponentSpec : StringSpec() {

    init {
        "labels follow the props" {

            withChart(chartConfig(datasets = arrayOf(dataset("A")))) { source, root ->

                source(chartConfig(labels = arrayOf("b", "c", "d"), datasets = arrayOf(dataset("A"))))

                eventually {
                    root.liveChart().data.labels.unsafeCast<Array<String>>().toList() shouldBe listOf("b", "c", "d")
                }
            }
        }

        "a dataset added later appears" {

            withChart(chartConfig(datasets = arrayOf(dataset("A")))) { source, root ->

                source(chartConfig(datasets = arrayOf(dataset("A"), dataset("B"))))

                eventually {
                    val chart = root.liveChart()

                    labelsOf(chart) shouldBe listOf("A", "B")
                    // Chart.js built a controller for it, i.e. the update ran with the new dataset
                    (chart.getDatasetMeta(1).controller != null) shouldBe true
                }
            }
        }

        "a dataset removed later disappears" {

            withChart(chartConfig(datasets = arrayOf(dataset("A"), dataset("B")))) { source, root ->

                source(chartConfig(datasets = arrayOf(dataset("A"))))

                eventually {
                    val chart = root.liveChart()

                    labelsOf(chart) shouldBe listOf("A")
                    chart.getSortedVisibleDatasetMetas().length.unsafeCast<Int>() shouldBe 1
                }
            }
        }

        "a dataset's own fields follow, on the same dataset object" {

            val initial = dataset("A") { borderColor = "red".unsafeCast<StringOrArray>() }

            withChart(chartConfig(datasets = arrayOf(initial))) { source, root ->

                val before = root.liveChart().data.datasets[0]

                source(
                    chartConfig(
                        datasets = arrayOf(dataset("Renamed") { borderColor = "blue".unsafeCast<StringOrArray>() })
                    )
                )

                eventually {
                    val chart = root.liveChart()
                    val after = chart.data.datasets[0]

                    (after === before) shouldBe true
                    after.label.unsafeCast<String>() shouldBe "Renamed"
                    after.borderColor.unsafeCast<String>() shouldBe "blue"
                    // Chart.js picked up the new label during the update
                    chart.getDatasetMeta(0).label.unsafeCast<String>() shouldBe "Renamed"
                }
            }
        }

        "a field the next dataset no longer has is removed" {

            val initial = dataset("A") { stepped = "middle" }

            withChart(chartConfig(datasets = arrayOf(initial))) { source, root ->

                source(chartConfig(datasets = arrayOf(dataset("A"))))

                eventually {
                    jsTypeOf(root.liveChart().data.datasets[0].stepped) shouldBe "undefined"
                }
            }
        }

        "the live dataset objects passed again in a different order" {

            withChart(chartConfig(datasets = arrayOf(dataset("A"), dataset("B")))) { source, root ->

                val live = root.liveChart().data.datasets.unsafeCast<Array<ChartConfig.ChartData.Dataset>>()

                source(chartConfig(datasets = arrayOf(live[1], live[0])))

                eventually {
                    labelsOf(root.liveChart()) shouldBe listOf("B", "A")
                }
            }
        }

        "a changed type rebuilds the chart" {

            withChart(chartConfig(type = "line", datasets = arrayOf(dataset("A")))) { source, root ->

                val before = root.liveChart()

                source(chartConfig(type = "bar", datasets = arrayOf(dataset("A"))))

                eventually {
                    val chart = root.liveChart()

                    chart.config.type.unsafeCast<String>() shouldBe "bar"
                    (chart === before) shouldBe false
                }
            }
        }

        "a decimated dataset still updates when it shrinks below the threshold" {

            // Decimation is undone before the merge and redone by the plugin; below the threshold the new points show
            val options = decimationOptions()

            withChart(chartConfig(datasets = arrayOf(dataset("A", points(200))), options = options)) { source, root ->

                // Precondition: decimation is really active, or the test proves nothing
                eventually { (root.liveChart().data.datasets[0]._decimated != null) shouldBe true }

                source(chartConfig(datasets = arrayOf(dataset("A", points(3))), options = options))

                eventually {
                    val data = root.liveChart().data.datasets[0].data.unsafeCast<Array<dynamic>>()

                    data.map { it.y.unsafeCast<Int>() } shouldBe listOf(0, 2, 4)
                }
            }
        }

        "a live decimated dataset passed again keeps its full data" {

            val options = decimationOptions()

            withChart(chartConfig(datasets = arrayOf(dataset("A", points(200))), options = options)) { source, root ->

                // Precondition: the visible data is the 20 samples, the raw data the 200 points
                eventually { root.liveChart().data.datasets[0].data.length.unsafeCast<Int>() shouldBe 20 }

                val live = root.liveChart().data.datasets.unsafeCast<Array<ChartConfig.ChartData.Dataset>>()

                // New labels only to see when the update has landed
                source(chartConfig(labels = arrayOf("x"), datasets = arrayOf(live[0]), options = options))

                eventually {
                    val chart = root.liveChart()

                    chart.data.labels.unsafeCast<Array<String>>().toList() shouldBe listOf("x")
                    chart.data.datasets[0]._data.length.unsafeCast<Int>() shouldBe 200
                }
            }
        }

        "the caller's config objects and arrays are not written to" {

            val first = chartConfig(datasets = arrayOf(dataset("A")))
            val second = chartConfig(labels = arrayOf("x", "y"), datasets = arrayOf(dataset("B"), dataset("C")))

            withChart(first) { source, root ->

                source(second)
                eventually { labelsOf(root.liveChart()) shouldBe listOf("B", "C") }

                // Passing the first config again shows the first config, not what the second wrote into it
                source(first)
                eventually {
                    val chart = root.liveChart()

                    labelsOf(chart) shouldBe listOf("A")
                    chart.data.labels.unsafeCast<Array<String>>().toList() shouldBe listOf("a", "b", "c")
                }

                listOf(first, second).forEach { config ->
                    // Chart.js writes `scales` onto the options object it is given
                    jsTypeOf(config.options.scales) shouldBe "undefined"
                }

                // Chart.js patches the data arrays it listens to
                jsTypeOf(first.data.datasets[0].data.asDynamic()._chartjs) shouldBe "undefined"

                first.data.labels.toList() shouldBe listOf("a", "b", "c")
                first.data.datasets.map { it.label } shouldBe listOf("A")
                second.data.labels.toList() shouldBe listOf("x", "y")
                second.data.datasets.map { it.label } shouldBe listOf("B", "C")
            }
        }

        "a config Chart.js cannot build does not block a later valid one" {

            withChart(chartConfig(type = "line", datasets = arrayOf(dataset("A")))) { source, root ->

                source(chartConfig(type = "no-such-type", datasets = arrayOf(dataset("A"))))

                // The failed chart must not stay registered on the canvas
                eventually { (root.liveChart() == null) shouldBe true }

                source(chartConfig(type = "bar", datasets = arrayOf(dataset("A"))))

                eventually { root.liveChart().config.type.unsafeCast<String>() shouldBe "bar" }
            }
        }

        "a category value missing from the labels does not reach the caller's labels" {

            val point = jsObject<dynamic> { x = "z"; y = 1 }
            val config = chartConfig(datasets = arrayOf(dataset("A", arrayOf<Any>(point).unsafeCast<Array<Number>>())))

            withChart(config) { _, root ->
                // Precondition: Chart.js's category scale appended "z" to the labels it was given
                eventually {
                    root.liveChart().data.labels.unsafeCast<Array<String>>().toList() shouldBe listOf("a", "b", "c", "z")
                }

                config.data.labels.toList() shouldBe listOf("a", "b", "c")
            }
        }

        "a category value missing from the xLabels does not reach the caller's xLabels" {

            val point = jsObject<dynamic> { x = "z"; y = 1 }
            val config = chartConfig(datasets = arrayOf(dataset("A", arrayOf<Any>(point).unsafeCast<Array<Number>>())))
                .apply { data.asDynamic().xLabels = arrayOf("p", "q") }

            withChart(config) { _, root ->
                // Precondition: the category scale prefers xLabels and appended "z" to the array it was given
                eventually {
                    root.liveChart().data.xLabels.unsafeCast<Array<String>>().toList() shouldBe listOf("p", "q", "z")
                }

                config.data.asDynamic().xLabels.unsafeCast<Array<String>>().toList() shouldBe listOf("p", "q")
            }
        }

        "other data fields follow the props, e.g. xLabels" {

            val initial = chartConfig(datasets = arrayOf(dataset("A"))).apply {
                data.asDynamic().xLabels = arrayOf("p", "q", "r")
            }

            withChart(initial) { source, root ->

                val next = chartConfig(datasets = arrayOf(dataset("A"))).apply {
                    data.asDynamic().xLabels = arrayOf("q", "r", "s")
                }

                source(next)

                eventually {
                    root.liveChart().data.xLabels.unsafeCast<Array<String>>().toList() shouldBe listOf("q", "r", "s")
                }

                // A field the next config no longer has is removed
                source(chartConfig(datasets = arrayOf(dataset("A"))))

                eventually { jsTypeOf(root.liveChart().data.xLabels) shouldBe "undefined" }
            }
        }

        "plugin state in \"_\" fields of a live dataset survives an update" {

            withChart(chartConfig(datasets = arrayOf(dataset("A")))) { source, root ->

                // As a third-party plugin would keep it
                root.liveChart().data.datasets[0]._pluginState = 42

                source(chartConfig(labels = arrayOf("x"), datasets = arrayOf(dataset("Renamed"))))

                eventually {
                    val dataset = root.liveChart().data.datasets[0]

                    dataset.label.unsafeCast<String>() shouldBe "Renamed"
                    dataset._pluginState.unsafeCast<Int>() shouldBe 42
                }
            }
        }

        "a caller's _data field does not replace the data" {

            val config = chartConfig(
                datasets = arrayOf(dataset("A", arrayOf(1, 2, 3)) { asDynamic()._data = arrayOf(9, 9, 9) })
            )

            withChart(config) { _, root ->
                root.liveChart().data.datasets[0].data.unsafeCast<Array<Int>>().toList() shouldBe listOf(1, 2, 3)
            }
        }

        "a decimated series that stops being decimatable shows its new data" {

            val options = decimationOptions()

            withChart(chartConfig(datasets = arrayOf(dataset("A", points(200))), options = options)) { source, root ->

                eventually { root.liveChart().data.datasets[0].data.length.unsafeCast<Int>() shouldBe 20 }

                // A bar series is not decimated, so the plugin leaves the dataset alone
                source(
                    chartConfig(
                        datasets = arrayOf(dataset("A", points(50)) { asDynamic().type = "bar" }),
                        options = options,
                    )
                )

                eventually { root.liveChart().data.datasets[0].data.length.unsafeCast<Int>() shouldBe 50 }
            }
        }
    }
}
