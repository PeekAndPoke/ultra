package de.peekandpoke.kraft.addons.signaturepad.js

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event

@Suppress("ClassName")
@JsModule("signature_pad")
@JsNonModule
external object signature_pad {
    interface Options {
        /** Radius of a single dot. Float or function */
        var dotSize: dynamic

        /** Minimum width of a line. Defaults to 0.5 */
        var minWidth: Double

        /** Maximum width of a line. Defaults to 2.5 */
        var maxWidth: Double

        /**
         * Draw the next point at most once per every x milliseconds. Set it to 0 to turn off throttling.
         * Defaults to 16
         */
        var throttle: Int

        /** Add the next point only if the previous one is farther than x pixels. Defaults to 5 */
        var minDistance: Int

        /**
         * Color used to clear the background.
         *
         * Can be any color format accepted by context.fillStyle.
         * Defaults to "rgba(0,0,0,0)" (transparent black).
         * Use a non-transparent color e.g. "rgb(255,255,255)" (opaque white) if you'd like to save signatures as
         * JPEG images.
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
    }

    @JsName("default")
    class SignaturePad(canvas: HTMLCanvasElement) {

        /**
         * Creates the signature pad with [options]
         */
        constructor(canvas: HTMLCanvasElement, options: Options)

        /**
         * Save image as PNG
         */
        fun toDataURL(): String

        /**
         * Save image as the given [mimeType].
         *
         * Possible mime types are 'image/png', 'image/jpeg', 'image/svg+xml'
         */
        fun toDataURL(mimeType: String): String

        /**
         * Save image as the given [mimeType] and the given [quality].
         *
         * Possible mime types are 'image/png', 'image/jpeg', 'image/svg+xml'
         */
        fun toDataURL(mimeType: String, quality: Double): String

        /**
         * Adds an event listener.
         *
         * Events are beginStroke, endStroke, beforeUpdateStroke, afterUpdateStroke
         */
        fun addEventListener(event: String, listener: ((Event) -> Unit))

        /**
         * Removes an event listener.
         *
         * Events are beginStroke, endStroke, beforeUpdateStroke, afterUpdateStroke
         */
        fun removeEventListener(event: String, listener: ((Event) -> Unit))

        /**
         * Clear the contents of the pad
         */
        fun clear()

        /**
         * Returns true if the pad is empty
         */
        fun isEmpty(): Boolean

        /**
         * Unbinds all event listeners
         */
        fun off()

        /**
         * Rebinds all event listeners
         */
        fun on()
    }
}
