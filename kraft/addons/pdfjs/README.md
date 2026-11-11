# Kraft Addon: pdf.js

Kotlin/JS wrapper for [pdf.js](https://mozilla.github.io/pdf.js/) (npm: `pdfjs-dist`), providing PDF rendering in Kraft
applications.

## Features

- `ScrollingPdfViewer`: every page in one scrolling column, zoomable
- `PagedPdfViewer`: one page at a time, with page navigation
- `PdfSource.Url`, `PdfSource.Base64` and `PdfSource.Data` (bytes; the caller's array is copied, not handed over)
- No CDN: pdf.js comes from the npm package (4.x) as its own chunk, and its worker is copied into the app's build
  output, served from the app's origin

## Example

```kotlin
class DocumentView(ctx: NoProps) : PureComponent(ctx) {
    private val pdf: PdfJsAddon? by subscribingTo(addons.pdfJs)

    override fun VDom.render() {
        ScrollingPdfViewer(
            src = PdfSource.Url("https://example.com/document.pdf"),
            options = ScrollingPdfViewer.Options(
                maxHeightLandscapeVh = 80,
                maxHeightPortraitVh = 80,
                scaleRange = 0.1..3.0,
            ),
            onChange = { state ->
                console.log("${state.doc?.numPages} pages, at ${state.scale}x")
            },
        )
    }
}
```

Register the addon with `pdfJs()` in your app's addons (eager), or `pdfJs(lazy = true)` to load pdf.js on first use.
The server must serve `.mjs` as `text/javascript`: the pdf.js worker is a module worker.

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-pdfjs</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: pdfjs-dist](https://www.npmjs.com/package/pdfjs-dist)
- [Documentation](https://peekandpoke.io/ultra/kraft/addons)
