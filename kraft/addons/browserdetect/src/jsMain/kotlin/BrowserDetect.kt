package de.peekandpoke.kraft.addons.browserdetect

import de.peekandpoke.kraft.addons.browserdetect.js.Bowser
import kotlinx.browser.window
import org.w3c.dom.MimeType
import org.w3c.dom.Navigator
import org.w3c.dom.asList

@Suppress("MemberVisibilityCanBePrivate")
class BrowserDetect private constructor(
    val navigator: Navigator,
) {
    companion object {

        fun forNavigator(navigator: Navigator): BrowserDetect = BrowserDetect(navigator)

        fun forCurrentBrowser(): BrowserDetect = forNavigator(window.navigator)
    }

    data class Browser(
        val name: String,
        val version: String,
    )

    data class OS(
        val name: String,
        val version: String,
        val versionName: String,
    )

    data class Platform(
        val type: String,
    )

    data class Engine(
        val name: String,
        val version: String,
    )

    private val bowser = Bowser.getParser(navigator.userAgent)

    fun getRawResult(): dynamic {
        return bowser
    }

    fun getBrowser(): Browser {
        return bowser.getBrowser().let {
            Browser(
                name = it.name.orUnknown(),
                version = it.version.orUnknown(),
            )
        }
    }

    fun getOs(): OS {
        return bowser.getOS().let {
            OS(
                name = it.name.orUnknown(),
                version = it.version.orUnknown(),
                versionName = it.versionName.orUnknown(),
            )
        }
    }

    fun getPlatform(): Platform {
        return bowser.getPlatform().let {
            Platform(
                type = it.type.orUnknown()
            )
        }
    }

    fun getEngine(): Engine {
        return bowser.getEngine().let {
            Engine(
                name = it.name.orUnknown(),
                version = it.version.orUnknown(),
            )
        }
    }

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
