package io.peekandpoke.kraft.addons.browserdetect

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.Navigator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private suspend fun eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> Unit,
) {
    val deadline = kotlinx.datetime.Clock.System.now().plus(timeout)
    while (true) {
        try {
            check()
            return
        } catch (e: Throwable) {
            if (kotlinx.datetime.Clock.System.now() > deadline) throw e
            delay(poll)
        }
    }
}

private const val CHROME_WIN =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
private const val FIREFOX_MAC =
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:121.0) Gecko/20100101 Firefox/121.0"
private const val SAFARI_IOS =
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) " +
            "Version/17.0 Mobile/15E148 Safari/604.1"

/** A minimal fake [Navigator] carrying only a [userAgent] — enough for bowser-based detection. */
private fun fakeNavigator(userAgent: String): Navigator {
    val obj: dynamic = js("({})")
    obj.userAgent = userAgent
    return obj.unsafeCast<Navigator>()
}

@Suppress("TestFunctionName")
private fun Tag.BrowserDetectUaComponent() = comp { BrowserDetectUaComponent(it) }

private class BrowserDetectUaComponent(ctx: NoProps) : PureComponent(ctx) {
    private val detect by subscribingTo(addons.browserDetect)

    override fun VDom.render() {
        div(classes = "result") {
            val a = detect
            if (a == null) {
                +"loading"
            } else {
                val chrome = a.forNavigator(fakeNavigator(CHROME_WIN)).getBrowser().name
                val firefox = a.forNavigator(fakeNavigator(FIREFOX_MAC)).getBrowser().name
                val win = a.forNavigator(fakeNavigator(CHROME_WIN)).getOs().name
                val mobile = a.forNavigator(fakeNavigator(SAFARI_IOS)).getPlatform().type
                +"$chrome|$firefox|$win|$mobile"
            }
        }
    }
}

/** Deterministic browser/OS/platform detection from fixed user-agent strings. */
class BrowserDetectUaSpec : StringSpec() {

    init {
        "detects browser, OS and platform from fixed user-agent strings" {
            TestBed.preact(
                appSetup = { addons { browserDetect() } },
                view = { BrowserDetectUaComponent() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "Chrome|Firefox|Windows|mobile"
                }
            }
        }
    }
}
