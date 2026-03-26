package io.peekandpoke.kraft.addons.browserdetect

import io.peekandpoke.kraft.addons.browserdetect.js.Bowser
import kotlinx.browser.window
import org.w3c.dom.MimeType
import org.w3c.dom.Navigator
import org.w3c.dom.asList

/** Detects browser, OS, platform, and engine details using the Bowser library. */
@Suppress("MemberVisibilityCanBePrivate")
class BrowserDetect private constructor(
    val navigator: Navigator,
) {
    companion object {
        /** Creates a [BrowserDetect] for the given [navigator]. */
        fun forNavigator(navigator: Navigator): BrowserDetect = BrowserDetect(navigator)

        /** Creates a [BrowserDetect] for the current browser window. */
        fun forCurrentBrowser(): BrowserDetect = forNavigator(window.navigator)
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

    private val bowser = Bowser.getParser(navigator.userAgent)

    /** Returns the raw Bowser parser result. */
    fun getRawResult(): dynamic {
        return bowser
    }

    /** Detects the browser name and version. */
    fun getBrowser(): Browser {
        return bowser.getBrowser().let {
            Browser(
                name = it.name.orUnknown(),
                version = it.version.orUnknown(),
            )
        }
    }

    /** Detects the operating system. */
    fun getOs(): OS {
        return bowser.getOS().let {
            OS(
                name = it.name.orUnknown(),
                version = it.version.orUnknown(),
                versionName = it.versionName.orUnknown(),
            )
        }
    }

    /** Detects the platform type. */
    fun getPlatform(): Platform {
        return bowser.getPlatform().let {
            Platform(
                type = it.type.orUnknown()
            )
        }
    }

    /** Detects the rendering engine. */
    fun getEngine(): Engine {
        return bowser.getEngine().let {
            Engine(
                name = it.name.orUnknown(),
                version = it.version.orUnknown(),
            )
        }
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
