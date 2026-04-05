package io.peekandpoke.kraft.addons.browserdetect

import kotlinx.browser.window
import org.w3c.dom.MimeType
import org.w3c.dom.Navigator
import org.w3c.dom.asList

/** Detects browser, OS, platform, and engine details using the Bowser library. */
@Suppress("MemberVisibilityCanBePrivate")
class BrowserDetect private constructor(
    val addon: BrowserDetectAddon,
    val navigator: Navigator,
) {
    companion object {
        /** Creates a [BrowserDetect] for the given [navigator]. */
        fun forNavigator(addon: BrowserDetectAddon, navigator: Navigator): BrowserDetect =
            BrowserDetect(addon, navigator)

        /** Creates a [BrowserDetect] for the current browser window. */
        fun forCurrentBrowser(addon: BrowserDetectAddon): BrowserDetect =
            forNavigator(addon, window.navigator)
    }

    /** Detected browser name and version. */
    data class Browser(
        val name: String,
        val version: String,
    )

    /** Detected operating system name, version, and version name. */
    data class OS(
        val name: String,
        val version: String,
        val versionName: String,
    )

    /** Detected platform type (e.g. "desktop", "mobile", "tablet"). */
    data class Platform(
        val type: String,
    )

    /** Detected rendering engine name and version. */
    data class Engine(
        val name: String,
        val version: String,
    )

    private val bowser: dynamic = addon.getParser(navigator.userAgent)

    /** Returns the raw Bowser parser result. */
    fun getRawResult(): dynamic {
        return bowser
    }

    /** Detects the browser name and version. */
    fun getBrowser(): Browser {
        val result: dynamic = bowser.getBrowser()
        return Browser(
            name = (result.name as? String).orUnknown(),
            version = (result.version as? String).orUnknown(),
        )
    }

    /** Detects the operating system. */
    fun getOs(): OS {
        val result: dynamic = bowser.getOS()
        return OS(
            name = (result.name as? String).orUnknown(),
            version = (result.version as? String).orUnknown(),
            versionName = (result.versionName as? String).orUnknown(),
        )
    }

    /** Detects the platform type. */
    fun getPlatform(): Platform {
        val result: dynamic = bowser.getPlatform()
        return Platform(
            type = (result.type as? String).orUnknown(),
        )
    }

    /** Detects the rendering engine. */
    fun getEngine(): Engine {
        val result: dynamic = bowser.getEngine()
        return Engine(
            name = (result.name as? String).orUnknown(),
            version = (result.version as? String).orUnknown(),
        )
    }

    /** Returns all MIME types supported by the browser. */
    fun getMimeTypes(): List<MimeType> {
        return navigator.mimeTypes.asList()
    }

    /**
     * Teken from https://github.com/featurist/browser-pdf-support/blob/master/index.js
     */
    fun supportsPdf(): Boolean {
        fun hasAcrobatInstalled(): Boolean {
            fun checkActiveXSupport(): Boolean {
                return try {
                    return js("!!(new ActiveXObject(\"AcroPDF.PDF\"))") as Boolean
                } catch (e: Throwable) {
                    try {
                        return js("!!(new ActiveXObject(\"PDF.PdfCtrl\"))") as Boolean
                    } catch (e: Throwable) {
                        false
                    }
                }
            }

            return checkActiveXSupport()
        }

        fun isIos(): Boolean {

            val msStream: Any? = window.asDynamic()["MSStream"]

            val regex = "iPad|iPhone|iPod".toRegex()
            val matches = regex.containsMatchIn(navigator.userAgent)

            return matches && msStream in listOf(undefined, null)
        }

        return window.navigator.mimeTypes.namedItem("application/pdf") != null ||
                isIos() ||
                hasAcrobatInstalled()
    }

    private fun String?.orUnknown() = this ?: "unknown"
}
