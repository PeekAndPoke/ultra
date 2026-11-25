# Kraft Addon: Marked

Kotlin/JS wrapper for [marked](https://marked.js.org/) (npm: `marked`)
with [DOMPurify](https://github.com/cure53/DOMPurify) sanitization, providing Markdown rendering in Kraft applications.

## Features

- Convert Markdown to HTML using the marked parser
- Sanitize output with DOMPurify to prevent XSS
- Call `markdown2html()` from a component that subscribes to the addon

## Example

```kotlin
private val marked: MarkedAddon? by subscribingTo(addons.marked)

unsafe {
    +marked?.markdown2html("## Hello\n\nThis is **bold**.").orEmpty()
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-marked</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: marked](https://www.npmjs.com/package/marked)
- [Documentation](https://peekandpoke.io/ultra/kraft/addons)
