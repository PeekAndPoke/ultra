# Kraft Addon: PrismJS

Kotlin/JS wrapper for [PrismJS](https://prismjs.com/) (npm: `prismjs`), providing syntax highlighting in Kraft
applications.

## Features

- A component per language: `PrismKotlin`, `PrismJava`, `PrismJavascript`, `PrismJson`, `PrismHtml`, `PrismCss`,
  `PrismXml`, `PrismRust`, `PrismTypescript`, `PrismAql` and more; a new `code` re-highlights the block
- Plugins: `lineNumbers()`, `copyToClipboard()`, `inlineColor()`, `showLanguage()`
- Prism runs in manual mode: it highlights only its own blocks, never the rest of the page
- No CDN: the theme and plugin stylesheets come from the npm package, copied into the app's build output and loaded
  when first needed

## Example

```kotlin
override fun VDom.render() {
    PrismKotlin("""
        fun greet(name: String) = "Hello, ${'$'}name!"
    """.trimIndent()) {
        lineNumbers()
        copyToClipboard()
    }
}
```

The `Prism` component loads Prism itself, so no `subscribingTo` is needed. An app with its own `.css` loader rule in
webpack adds `dependency: { not: ['url'] }` to it, so the stylesheets stay plain files.

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-prismjs</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: prismjs](https://www.npmjs.com/package/prismjs)
- [Documentation](https://peekandpoke.io/ultra/kraft/addons)
