# Kraft Addon: Chart.js

Kotlin/JS wrapper for [Chart.js](https://www.chartjs.org/) 4 (npm: `chart.js`) for Kraft applications.

## Features

- Every Chart.js chart type, configured through typed `ChartConfig` externals and `jsObject { }`
- Live updates: a new config updates the chart in place (labels, datasets, options); a new `type` rebuilds it

## Example

Register `chartJs()` in `addons { }`, then render:

```kotlin
ChartJs(chartJsData {
    jsObject {
        type = "bar"
        data = jsObject {
            labels = arrayOf("Jan", "Feb", "Mar")
            datasets = arrayOf(jsObject {
                label = "Revenue"
                data = arrayOf(120, 190, 300)
            })
        }
    }
})
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
