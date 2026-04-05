package io.peekandpoke.kraft.addons.browserdetect

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Polls until [check] passes or [timeout] is reached. */
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

// Test component: shows "loading" until the addon is ready, then "ready"
@Suppress("FunctionName")
private fun Tag.AddonStatusComponent() = comp { AddonStatusComponent(it) }

private class AddonStatusComponent(ctx: NoProps) : PureComponent(ctx) {
    private val browserDetectAddon: BrowserDetectAddon? by subscribingTo(addons.browserDetect)

    override fun VDom.render() {
        div(classes = "addon-status") {
            if (browserDetectAddon != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Test component: when addon is ready, detects browser and renders its name
@Suppress("FunctionName")
private fun Tag.BrowserInfoComponent() = comp { BrowserInfoComponent(it) }

private class BrowserInfoComponent(ctx: NoProps) : PureComponent(ctx) {
    private val browserDetectAddon: BrowserDetectAddon? by subscribingTo(addons.browserDetect)

    override fun VDom.render() {
        div(classes = "browser-info") {
            val addon = browserDetectAddon

            if (addon != null) {
                val detect = addon.forCurrentBrowser()
                +detect.getBrowser().name
            } else {
                +"loading"
            }
        }
    }
}

class BrowserDetectAddonSpec : StringSpec() {

    init {
        "browserdetect addon loads and provides BrowserDetectAddon" {
            TestBed.preact(
                appSetup = {
                    addons {
                        browserDetect()
                    }
                },
                view = {
                    AddonStatusComponent()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "BrowserDetect detects browser info" {
            TestBed.preact(
                appSetup = {
                    addons {
                        browserDetect()
                    }
                },
                view = {
                    BrowserInfoComponent()
                },
            ) { root ->
                eventually {
                    val browserName = root.selectCss(".browser-info").textContent()
                    browserName shouldNotBe "loading"
                    browserName shouldNotBe "unknown"
                }
            }
        }
    }
}
