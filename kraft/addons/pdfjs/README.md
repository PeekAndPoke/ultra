# Kraft Addon: pdf.js

Kotlin/JS wrapper for [pdf.js](https://mozilla.github.io/pdf.js/) (npm: `pdfjs-dist`), providing PDF rendering in Kraft
applications.

## Features

- Render PDF documents directly in the browser
- Scrolling viewer for continuous page display
- Paged viewer with page navigation controls

## Example

```kotlin
pdfViewer {
    url = "/documents/report.pdf"
    mode = PdfViewerMode.scrolling
    initialPage = 1
}
```

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
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/pdfjs)
