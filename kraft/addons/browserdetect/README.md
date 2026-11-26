# Kraft Addon: Browser Detect

Kotlin/JS wrapper for [Bowser](https://github.com/bowser-js/bowser) (npm: `bowser`), providing browser and OS detection
in Kraft applications.

## Features

- Detect browser name, version, and rendering engine
- Detect operating system and platform (desktop, mobile, tablet)
- Query browser capabilities for conditional logic

## Example

```kotlin
private val bd: BrowserDetectAddon? by subscribingTo(addons.browserDetect)

val detect = bd?.forCurrentBrowser()

println(detect?.getBrowser()?.name)   // e.g., "Chrome"
println(detect?.getBrowser()?.version) // e.g., "120.0"
println(detect?.getOs()?.name)        // e.g., "macOS"
println(detect?.getPlatform()?.type)  // e.g., "desktop"
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-browserdetect</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: bowser](https://www.npmjs.com/package/bowser)
- [Documentation](https://peekandpoke.io/ultra/kraft/addons)
