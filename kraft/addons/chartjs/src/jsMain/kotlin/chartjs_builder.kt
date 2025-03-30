package de.peekandpoke.kraft.addons.chartjs

import kotlinx.css.Color

@Suppress("unused")
interface SingleOrArray<T>

typealias StringOrArray = SingleOrArray<String>

typealias NumberOrArray = SingleOrArray<Number>

fun chartJsData(builder: ChartJsDataBuilder.() -> ChartConfig): ChartConfig {
    return ChartJsDataBuilder().builder()
}

class ChartJsDataBuilder {

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

    fun nextColor() = Color(colors[nextColorIdx]).also {
        nextColorIdx = (nextColorIdx + 1) % colors.size
    }

    fun <T> value(value: T) = value.unsafeCast<SingleOrArray<T>>()

    fun <T> value(value: Array<T>) = value.unsafeCast<SingleOrArray<T>>()

    fun <T> value(vararg value: T) = value.unsafeCast<SingleOrArray<T>>()

    fun numberLabels(count: Int): Array<String> = ChartJsUtils.numberLabels(count)

    fun numberLabels(range: IntRange): Array<String> = ChartJsUtils.numberLabels(range)
}
