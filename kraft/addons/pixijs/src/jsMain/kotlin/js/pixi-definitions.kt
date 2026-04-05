@file:Suppress("FunctionName", "unused")

package io.peekandpoke.kraft.addons.pixijs.js

import org.w3c.dom.HTMLCanvasElement
import kotlin.js.Promise

/**
 * Minimal type-only declarations for pixi.js v8.
 *
 * Instances are created via [io.peekandpoke.kraft.addons.pixijs.PixiJsAddon].
 * The JS library is loaded dynamically — no static @JsModule import.
 *
 * Based on: https://pixijs.download/release/docs/index.html
 */

/** Options for [Application.init]. */
external interface ApplicationOptions {
    var width: Int
    var height: Int
    var backgroundColor: dynamic // Number | String
    var antialias: Boolean
    var resolution: Double
    var autoDensity: Boolean
    var preference: String // "webgl" | "webgpu"
    var resizeTo: dynamic // Window | HTMLElement
}

/** Main PixiJS application — manages renderer, stage, and ticker. */
external class Application {
    /** Root display container. */
    val stage: Container

    /** The canvas element the renderer draws to. */
    val canvas: HTMLCanvasElement

    /** The ticker drives the animation loop. */
    val ticker: Ticker

    /** Width of the rendered area. */
    val screen: Rectangle

    /** Initializes the application asynchronously. */
    fun init(options: ApplicationOptions = definedExternally): Promise<Unit>

    /** Renders the current stage. */
    fun render()

    /** Destroys the application and frees resources. */
    fun destroy(rendererDestroy: Boolean = definedExternally, options: dynamic = definedExternally)
}

/** Visible rectangle area. */
external class Rectangle {
    var x: Double
    var y: Double
    var width: Double
    var height: Double
}

/** Drives the animation loop with `add(callback)`. */
external class Ticker {
    /** Time since last tick (multiplied by `FRAME_TIME / 16.666`). */
    val deltaTime: Double

    /** Milliseconds since last tick. */
    val deltaMS: Double

    /** Adds a listener to the update loop. */
    fun add(callback: (Ticker) -> Unit)

    /** Removes a listener. */
    fun remove(callback: (Ticker) -> Unit)

    /** Stops the ticker. */
    fun stop()

    /** Starts the ticker. */
    fun start()
}

/** Base display object — can be added to the stage. */
external open class Container {
    var x: Double
    var y: Double
    var visible: Boolean
    var alpha: Double
    var rotation: Double
    val children: Array<Container>

    /** Adds a child to this container. */
    fun addChild(child: Container): Container

    /** Removes a child from this container. */
    fun removeChild(child: Container): Container

    /** Removes all children. */
    fun removeChildren()

    /** Removes this object from its parent. */
    fun removeFromParent()

    /** Destroys this display object. */
    fun destroy()
}

/** Vector drawing API for shapes. */
external class Graphics : Container {
    /** Draws a rectangle. */
    fun rect(x: Double, y: Double, width: Double, height: Double): Graphics

    /** Draws a circle. */
    fun circle(x: Double, y: Double, radius: Double): Graphics

    /** Draws a rounded rectangle. */
    fun roundRect(x: Double, y: Double, width: Double, height: Double, radius: Double): Graphics

    /** Fills the current path. */
    fun fill(color: dynamic = definedExternally): Graphics

    /** Strokes the current path. */
    fun stroke(style: dynamic = definedExternally): Graphics

    /** Clears all drawn shapes. */
    fun clear(): Graphics
}

/** Text display object. */
external class Text(options: TextOptions = definedExternally) : Container {
    var text: String
    val style: dynamic
}

/** Options for creating a [Text] object. */
external interface TextOptions {
    var text: String
    var style: dynamic // TextStyle — loose typing
}
