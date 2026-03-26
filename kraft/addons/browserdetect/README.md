# Kraft Addon: Browser Detect

Kotlin/JS wrapper for [Bowser](https://github.com/bowser-js/bowser) (npm: `bowser`), providing browser and OS detection
in Kraft applications.

## Features

- Detect browser name, version, and rendering engine
- Detect operating system and platform (desktop, mobile, tablet)
- Query browser capabilities for conditional logic

## Example

```kotlin
val browser = BrowserDetect.detect()

println(browser.browserName)    // e.g., "Chrome"
println(browser.browserVersion) // e.g., "120.0"
println(browser.osName)         // e.g., "macOS"
println(browser.platformType)   // e.g., "desktop"
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
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/browserdetect)
