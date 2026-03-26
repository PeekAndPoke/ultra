# Kraft Addon: Chart.js

Kotlin/JS wrapper for [Chart.js](https://www.chartjs.org/) (npm: `chart.js`), providing type-safe chart rendering in
Kraft applications.

## Features

- Render bar, line, pie, doughnut, radar, polar area, bubble, and scatter charts
- Type-safe configuration DSL for datasets, scales, tooltips, and animations
- Reactive updates when chart data changes

## Example

```kotlin
chartJs {
    chartConfig {
        type = ChartType.bar
        data {
            labels = listOf("Jan", "Feb", "Mar")
            datasets {
                dataset {
                    label = "Revenue"
                    data = listOf(120, 190, 300)
                }
            }
        }
    }
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-chartjs</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: chart.js](https://www.npmjs.com/package/chart.js)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/chartjs)
