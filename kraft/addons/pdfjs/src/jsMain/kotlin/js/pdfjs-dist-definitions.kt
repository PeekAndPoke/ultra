@file:Suppress("PropertyName", "ClassName")

package de.peekandpoke.kraft.addons.pdfjs.js

import js.objects.Object
import org.khronos.webgl.Uint8Array
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.js.Promise

external val pdfjsLib: PdfjsLib

external interface PdfjsLib {

    interface GlobalWorkerOptionsDef {
        var workerSrc: String
    }

    val GlobalWorkerOptions: GlobalWorkerOptionsDef

    /**
     * https://github.com/mozilla/pdf.js/blob/af6aacfc0eaa70f254cd6158e5cd9cbc23fb13cf/src/display/api.js#L133
     */
    interface GetDocumentParameters {
        /**
         * The URL of the PDF
         */
        var url: String

        /**
         * Binary PDF data.
         *
         * Use typed arrays (Uint8Array) to improve the memory usage. If PDF data is
         * BASE64-encoded, use `atob()` to convert it to a binary string first.
         */
        var data: Uint8Array

        /**
         * Basic authentication headers.
         */
        var httpHeaders: Object

        /**
         * Indicates whether or not cross-site Access-Control requests should be made using credentials such
         * as cookies or authorization headers. The default is `false`.
         */
        var withCredentials: Boolean

        /**
         * For decrypting password-protected PDFs.
         */
        var password: String
    }

    interface PDFDocumentLoadingTask {
        val promise: Promise<PDFDocumentProxy>
    }

    /**
     * https://github.com/mozilla/pdf.js/blob/af6aacfc0eaa70f254cd6158e5cd9cbc23fb13cf/src/display/api.js#L709
     */
    interface PDFDocumentProxy {
        val numPages: Int

        val fingerprints: Array<String?>

        fun getPage(page: Int): Promise<PDFPageProxy>
    }

    /**
     * https://github.com/mozilla/pdf.js/blob/af6aacfc0eaa70f254cd6158e5cd9cbc23fb13cf/src/display/api.js#L1219
     */
    interface PDFPageProxy {

        interface GetViewportOptions {
            var scale: Number
        }

        interface RenderOptions {
            var canvasContext: CanvasRenderingContext2D
            var viewport: PageViewport
        }

        /**
         * Page number of the page. First page is 1.
         */
        val pageNumber: Int

        /**
         * An array of the visible portion of the PDF page in user space units [x1, y1, x2, y2].
         */
        val view: Array<Number>

        fun getViewport(options: GetViewportOptions): PageViewport

        fun render(options: RenderOptions): RenderTask
    }

    /**
     * https://github.com/mozilla/pdf.js/blob/af6aacfc0eaa70f254cd6158e5cd9cbc23fb13cf/src/display/display_utils.js#L158
     */
    interface PageViewport {
        var viewBox: Array<Number>
        var width: Int
        var height: Int
        var scale: Number
        var rotation: Number
        var offsetX: Number
        var offsetY: Number
        var dontFlip: Boolean
    }

    /**
     * https://github.com/mozilla/pdf.js/blob/af6aacfc0eaa70f254cd6158e5cd9cbc23fb13cf/src/display/api.js#L3108
     */
    interface RenderTask {
        val promise: Promise<Unit>
    }

    fun getDocument(src: String): PDFDocumentLoadingTask

    fun getDocument(src: GetDocumentParameters): PDFDocumentLoadingTask
}

