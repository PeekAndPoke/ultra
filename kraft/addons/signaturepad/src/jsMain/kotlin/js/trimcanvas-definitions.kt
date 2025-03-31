package de.peekandpoke.kraft.addons.signaturepad.js

import org.w3c.dom.HTMLCanvasElement

@Suppress("ClassName")
@JsModule("trim-canvas")
@JsNonModule
external object trim_canvas {

    @JsName("default")
    fun trimCanvas(canvas: HTMLCanvasElement): HTMLCanvasElement
}
