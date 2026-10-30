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
    var backgroundAlpha: Double
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

    /**
     * Resizes the renderer to the `resizeTo` target's size now; does nothing while `resizeTo` is not set. Added by
     * pixi's ResizePlugin in [init]. `resizeTo` follows the window's `resize` event alone, so an element resized by
     * layout needs this, or `followElementSize(element)` from the addon package.
     */
    fun resize()

    /** Like [resize], on the next animation frame; several calls in one frame resize once. */
    fun queueResize()

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
open external class Container {
    var x: Double
    var y: Double
    var visible: Boolean
    var alpha: Double
    var rotation: Double
    val children: Array<Container>

    /** The scale along x and y: `scale.set(2.0)` doubles both. */
    val scale: ObservablePoint

    /**
     * The draw order among siblings: a higher one draws later, on top, once the parent sorts by it. Set while the node
     * sits in its parent, or before an [addChild], it turns the parent's [sortableChildren] on and marks the order to
     * be sorted. Before an [addChildAt] it does neither (pixi 8.22): set the parent's [sortableChildren] before the
     * [addChildAt]. Afterwards neither helps: setting the value it already has does nothing (pixi ignores an equal
     * one), and turning [sortableChildren] on does not mark the order.
     */
    var zIndex: Double

    /** Whether the container sorts its children by [zIndex] before it renders (stable: equal ones keep their order). */
    var sortableChildren: Boolean

    /** Adds a child to this container. */
    fun addChild(child: Container): Container

    /**
     * Puts [child] at [index] among the children: a child of this one moves there, a child of another leaves that
     * first. Unlike [addChild] it does not turn [sortableChildren] on for a child that has a [zIndex] already.
     */
    fun addChildAt(child: Container, index: Int): Container

    /**
     * Sorts the children by [zIndex] now, if a zIndex change or an add to a sortable container marked the order;
     * otherwise does nothing. The renderer calls it on its own; a test or a layout that reads [children] may.
     */
    fun sortChildren()

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

    /** Adds a polygon through [points] (x0, y0, x1, y1, ...) to the path, closed unless [close] is false. */
    fun poly(points: Array<Double>, close: Boolean = definedExternally): Graphics

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

    /** Where the text's position sits in it: (0, 0) its top left, (0.5, 0.5) its center. */
    val anchor: ObservablePoint
}

/** A point whose change updates its owner's transform (`scale`, `anchor`). */
external interface ObservablePoint {
    var x: Double
    var y: Double

    /** Sets both; with [y] left out, both are [x]. */
    fun set(x: Double, y: Double = definedExternally)
}

/** Options for creating a [Text] object. */
external interface TextOptions {
    var text: String
    var style: dynamic // TextStyle — loose typing
}
