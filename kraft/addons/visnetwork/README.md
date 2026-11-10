# Kraft Addon: vis-network

Kotlin/JS wrapper for [vis-network](https://visjs.github.io/vis-network/docs/network/) (npm: `vis-network`) for Kraft
applications: network graphs with a physics layout.

## Features

- `VisNetwork(data, options, height, onReady)`: draws a network, updates it when `data` or `options` change, destroys
  it on unmount
- Works with the `visNetwork()` addon registered, and without it: the component then loads the library itself
- Typed `VisNode`, `VisEdge`, `VisNetworkOptions` externals, built with `jsObject { }`

## Example

```kotlin
VisNetwork(
    data = jsObject {
        nodes = arrayOf(jsObject { id = 1; label = "Api" }, jsObject { id = 2; label = "Repo" })
        edges = arrayOf(jsObject { from = 1; to = 2; arrows = "to" })
    },
    options = jsObject { physics = jsObject<dynamic> { solver = "forceAtlas2Based" } },
)
```

Labels are drawn on a canvas. A string `title` is shown as text, an element as HTML: never build one from untrusted
input.

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-visnetwork</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [vis-network documentation](https://visjs.github.io/vis-network/docs/network/)
