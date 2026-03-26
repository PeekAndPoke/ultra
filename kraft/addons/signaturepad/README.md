# Kraft Addon: Signature Pad

Kotlin/JS wrapper for [signature_pad](https://github.com/szimek/signature_pad) (npm: `signature_pad`), providing
handwritten signature capture in Kraft applications.

## Features

- Capture handwritten signatures via mouse or touch input
- Export signatures as PNG, JPG, or SVG
- Configure pen color, width, and background color

## Example

```kotlin
signaturePad {
    penColor = "black"
    backgroundColor = "white"
    onEnd { pad ->
        val dataUrl = pad.toDataURL("image/png")
        // use the signature image
    }
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-signaturepad</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: signature_pad](https://www.npmjs.com/package/signature_pad)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/signaturepad)
