package io.peekandpoke.kraft.addons.chartjs

import kotlinx.css.Color

/** Marker interface representing a value that can be either a single [T] or an array of [T]. */
@Suppress("unused")
interface SingleOrArray<T>

/** A single string or an array of strings for Chart.js properties. */
typealias StringOrArray = SingleOrArray<String>

/** A single number or an array of numbers for Chart.js properties. */
typealias NumberOrArray = SingleOrArray<Number>

/** DSL entry point for building a [ChartConfig] with [ChartJsDataBuilder]. */
fun chartJsData(builder: ChartJsDataBuilder.() -> ChartConfig): ChartConfig {
    return ChartJsDataBuilder().builder()
}

/** Builder for constructing Chart.js data configurations with color cycling and label helpers. */
class ChartJsDataBuilder {

    /** Default color palette for chart datasets. */
    val colors = arrayOf(
        "#4dc9f6",
        "#f67019",
        "#f53794",
        "#537bc4",
        "#acc236",
        "#166a8f",
        "#00a950",
        "#58595b",
        "#8549ba"
    )

    private var nextColorIdx = 0

    /** Returns the next color from the palette, cycling through automatically. */
    fun nextColor() = Color(colors[nextColorIdx]).also {
        nextColorIdx = (nextColorIdx + 1) % colors.size
    }

    /** Wraps a single [value] as a [SingleOrArray]. */
    fun <T> value(value: T) = value.unsafeCast<SingleOrArray<T>>()

    /** Wraps an array [value] as a [SingleOrArray]. */
    fun <T> value(value: Array<T>) = value.unsafeCast<SingleOrArray<T>>()

    /** Wraps vararg values as a [SingleOrArray]. */
    fun <T> value(vararg value: T) = value.unsafeCast<SingleOrArray<T>>()

    /** Creates numeric labels from 1 to [count]. */
    fun numberLabels(count: Int): Array<String> = ChartJsUtils.numberLabels(count)

    /** Creates numeric labels for the given [range]. */
    fun numberLabels(range: IntRange): Array<String> = ChartJsUtils.numberLabels(range)
}
