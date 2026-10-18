package io.peekandpoke.kraft.addons.chartjs

/**
 * A copy of [config] for the chart to own, one level deep.
 *
 * Chart.js keeps the objects it is given and writes into them (options, labels, dataset fields, data arrays),
 * so these are copied. Point objects and nested option objects are shared with the caller.
 */
internal fun ownedCopy(config: ChartConfig): ChartConfig {
    val data = config.data.unsafeCast<ChartConfig.ChartData?>()

    return shallowCopy(config).apply {
        this.data = ownedData(data).apply {
            labels = data?.labels.unsafeCast<Array<String>?>()?.copyOf() ?: emptyArray()
            datasets = (data?.datasets.unsafeCast<Array<ChartConfig.ChartData.Dataset>?>() ?: emptyArray())
                .map { ownedDataset(it) }
                .toTypedArray()
        }
        // Missing options stay missing: Chart.js then creates its own
        options = config.options.unsafeCast<ChartConfig.Options?>()?.let { shallowCopy(it) }
            .unsafeCast<ChartConfig.Options>()
    }
}

/**
 * Makes this live chart follow [next]: options, data fields (labels, ...) and datasets.
 *
 * Existing dataset objects are kept and given exactly the fields of the next dataset at the same index, because
 * Chart.js tracks a dataset by object identity — a new object would lose its state and animate from scratch.
 */
internal fun Chart.mergeFrom(next: ChartConfig) {
    val owned = ownedCopy(next)

    options = owned.options
    replaceDataFields(target = data, source = owned.data)

    val current = data.datasets
    val incoming = owned.data.datasets

    incoming.forEachIndexed { idx, dataset ->
        when (val existing = current.getOrNull(idx)) {
            null -> current.asDynamic().push(dataset)
            else -> {
                undoDecimation(existing)
                replaceOwnProperties(target = existing, source = dataset)
            }
        }
    }

    // Datasets the next config no longer has
    current.asDynamic().length = incoming.size
}

/** Copies the fields of a chart's `data` except `datasets`, each array with its own copy (`xLabels`, ...). */
@Suppress("UNUSED_PARAMETER")
private fun ownedData(source: ChartConfig.ChartData?): ChartConfig.ChartData = js(
    "(function () {" +
            " var copy = {};" +
            " if (source) Object.keys(source).forEach(function (k) {" +
            "  if (k !== 'datasets') copy[k] = Array.isArray(source[k]) ? source[k].slice() : source[k];" +
            " });" +
            " return copy;" +
            " })()"
).unsafeCast<ChartConfig.ChartData>()

/** Gives the live `data` [target] the fields of [source] except `datasets`, which are merged one by one. */
@Suppress("UNUSED_PARAMETER")
private fun replaceDataFields(target: ChartConfig.ChartData, source: ChartConfig.ChartData) {
    js(
        "Object.keys(target).forEach(function (k) {" +
                " if (k !== 'datasets' && !Object.prototype.hasOwnProperty.call(source, k)) delete target[k];" +
                " })"
    )
    js(
        "Object.keys(source).forEach(function (k) {" +
                " if (k !== 'datasets') target[k] = source[k];" +
                " })"
    )
}

/**
 * Gives [target] the own enumerable properties of [source], keeping the [target] object itself.
 *
 * Keys starting with "_" survive: plugins keep their state there (decimation's is undone before the merge).
 */
@Suppress("UNUSED_PARAMETER")
private fun replaceOwnProperties(target: dynamic, source: dynamic) {
    js(
        "Object.keys(target).forEach(function (k) {" +
                " if (k.charAt(0) !== '_' && !Object.prototype.hasOwnProperty.call(source, k)) delete target[k];" +
                " })"
    )
    js("Object.assign(target, source)")
}

/**
 * Turns a dataset the decimation plugin worked on back into a plain one, as the plugin's own cleanup does.
 *
 * The plugin turns `data` into an accessor over `_data` / `_decimated` and undoes it only when the visible points no
 * longer exceed its threshold, or decimation is disabled. A dataset that stops being decimatable otherwise (switched to "bar", parsing turned on)
 * would keep reading its old decimated points. The plugin redoes the accessor on the next update where it applies.
 */
@Suppress("UNUSED_PARAMETER")
private fun undoDecimation(dataset: ChartConfig.ChartData.Dataset) {
    js(
        "(function () {" +
                " var d = Object.getOwnPropertyDescriptor(dataset, 'data');" +
                " if (!d || !d.get) return;" +
                " var raw = dataset._data;" +
                " delete dataset._decimated;" +
                " delete dataset._data;" +
                " Object.defineProperty(dataset, 'data'," +
                "  { configurable: true, enumerable: true, writable: true, value: raw });" +
                " })()"
    )
}

/**
 * Copies a dataset without "_" fields (reserved for Chart.js), with its own data array.
 *
 * A dataset the decimation plugin worked on reads its `data` as the decimated subset; the raw data is then `_data`.
 * The accessor is the tell: a `_data` field from anywhere else (e.g. JSON) cannot create one.
 */
@Suppress("UNUSED_PARAMETER")
private fun ownedDataset(source: ChartConfig.ChartData.Dataset): ChartConfig.ChartData.Dataset = js(
    "(function () {" +
            " var copy = {};" +
            " Object.keys(source).forEach(function (k) { if (k.charAt(0) !== '_') copy[k] = source[k]; });" +
            " var d = Object.getOwnPropertyDescriptor(source, 'data');" +
            " if (d && d.get && Object.prototype.hasOwnProperty.call(source, '_data')) copy.data = source._data;" +
            " if (Array.isArray(copy.data)) copy.data = copy.data.slice();" +
            " return copy;" +
            " })()"
).unsafeCast<ChartConfig.ChartData.Dataset>()

@Suppress("UNUSED_PARAMETER")
private fun <T> shallowCopy(source: T): T = js("Object.assign({}, source)").unsafeCast<T>()
