# Kraft Addon: vis-network

Kotlin/JS wrapper for [vis-network](https://visjs.github.io/vis-network/docs/network/) (npm: `vis-network`) for Kraft
applications: network graphs with a physics layout.

## Features

- `VisNetwork(data, options, height, onReady)`: draws a network; a new `data` object (by identity) resets it and its
  selection, new `options` restart its layout (build both once, not in `render()`); destroys it on unmount
- Works with the `visNetwork()` addon registered, and without it: the component then loads the library itself
- Typed `VisNode`, `VisEdge`, `VisNetworkOptions` externals, built with `jsObject { }`

## Example

```kotlin
class ServiceGraph(ctx: NoProps) : PureComponent(ctx) {
    // Built once: new data resets the network and its selection, new options restart its layout
    private val data = jsObject<VisNetworkData> {
        nodes = arrayOf(jsObject { id = 1; label = "Api" }, jsObject { id = 2; label = "Repo" })
        edges = arrayOf(jsObject { from = 1; to = 2; arrows = "to" })
    }
    private val options = jsObject<VisNetworkOptions> {
        physics = jsObject<dynamic> { solver = "forceAtlas2Based" }
    }

    override fun VDom.render() {
        VisNetwork(data = data, options = options, height = "500px")
    }
}
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
