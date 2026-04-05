package io.peekandpoke.kraft.addons.signaturepad.js

import org.w3c.dom.events.Event

/**
 * Type declarations for signature_pad.js v5.x.
 *
 * Instances are created via [io.peekandpoke.kraft.addons.signaturepad.SignaturePadAddon.create].
 * The JS library is loaded dynamically — no static @JsModule import.
 */
external class SignaturePadJs {

    /**
     * Options for configuring a SignaturePad instance.
     *
     * All fields from PointGroupOptions are optional (Partial<PointGroupOptions>).
     */
    interface Options {
        /** Radius of a single dot. Float or function */
        var dotSize: dynamic

        /** Minimum width of a line. Defaults to 0.5 */
        var minWidth: Double

        /** Maximum width of a line. Defaults to 2.5 */
        var maxWidth: Double

        /**
         * Draw the next point at most once per every x milliseconds.
         * Set it to 0 to turn off throttling. Defaults to 16.
         */
        var throttle: Int

        /** Add the next point only if the previous one is farther than x pixels. Defaults to 5 */
        var minDistance: Int

        /**
         * Color used to clear the background.
         *
         * Can be any color format accepted by context.fillStyle.
         * Defaults to "rgba(0,0,0,0)" (transparent black).
         * Use a non-transparent color e.g. "rgb(255,255,255)" (opaque white)
         * if you'd like to save signatures as JPEG images.
         */
        var backgroundColor: String

        /**
         * Color used to draw the lines.
         *
         * Can be any color format accepted by context.fillStyle. Defaults to "black".
         */
        var penColor: String

        /** Weight used to modify new velocity based on the previous velocity. Defaults to 0.7. */
        var velocityFilterWeight: Double

        /** Composite operation for drawing. Defaults to "source-over". */
        var compositeOperation: String
    }

    /** Save image as PNG data URL */
    fun toDataURL(): String

    /**
     * Save image as data URL with the given [mimeType].
     *
     * Supported: 'image/png', 'image/jpeg', 'image/svg+xml'
     */
    fun toDataURL(mimeType: String): String

    /**
     * Save image as data URL with the given [mimeType] and [quality] (for JPEG).
     */
    fun toDataURL(mimeType: String, quality: Double): String

    /** Export signature as SVG string. */
    fun toSVG(): String

    /**
     * Adds an event listener.
     *
     * Supported events: beginStroke, endStroke, beforeUpdateStroke, afterUpdateStroke
     */
    fun addEventListener(event: String, listener: ((Event) -> Unit))

    /**
     * Removes an event listener.
     */
    fun removeEventListener(event: String, listener: ((Event) -> Unit))

    /** Clear the contents of the pad */
    fun clear()

    /** Redraw the pad contents from stored point data */
    fun redraw()

    /** Returns true if the pad is empty */
    fun isEmpty(): Boolean

    /** Unbinds all event listeners */
    fun off()

    /** Rebinds all event listeners */
    fun on()

    /** Returns the stored point group data */
    fun toData(): Array<dynamic>

    /** Loads point group data into the pad */
    fun fromData(pointGroups: Array<dynamic>, options: dynamic = definedExternally)
}
