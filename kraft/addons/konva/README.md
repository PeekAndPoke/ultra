# Kraft Addon: Konva

Kotlin/JS wrapper for [Konva](https://konvajs.org/) (npm: `konva`), providing 2D canvas graphics in Kraft applications.

## Features

- Draw shapes (rectangles, circles, lines, polygons, text) on an HTML5 canvas
- Support for layers, groups, transformations, and hit detection
- Animations and tweening

## Example

```kotlin
konvaStage {
    width = 500
    height = 400
    layer {
        circle {
            x = 100.0
            y = 100.0
            radius = 50.0
            fill = "blue"
        }
        rect {
            x = 200.0
            y = 50.0
            width = 120.0
            height = 80.0
            fill = "red"
        }
    }
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-konva</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: konva](https://www.npmjs.com/package/konva)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/konva)
