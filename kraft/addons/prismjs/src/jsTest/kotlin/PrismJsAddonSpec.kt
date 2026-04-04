package io.peekandpoke.kraft.addons.prismjs

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

// Component that subscribes to the prismjs addon and renders loading/ready status
@Suppress("TestFunctionName")
private fun Tag.PrismJsStatus() = comp { PrismJsStatusComponent(it) }

private class PrismJsStatusComponent(ctx: NoProps) : PureComponent(ctx) {
    private val prismJsAddon by subscribingTo(addons.prismJs)

    override fun VDom.render() {
        div(classes = "addon-status") {
            val addon = prismJsAddon
            if (addon != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Component that checks prismJsInternals.prism after the addon loads
@Suppress("TestFunctionName")
private fun Tag.PrismInternalsCheck() = comp { PrismInternalsCheckComponent(it) }

private class PrismInternalsCheckComponent(ctx: NoProps) : PureComponent(ctx) {
    private val prismJsAddon by subscribingTo(addons.prismJs)

    override fun VDom.render() {
        div(classes = "addon-status") {
            val addon = prismJsAddon
            if (addon != null && prismJsInternals.prism != null) {
                +"prism-loaded"
            } else {
                +"loading"
            }
        }
    }
}

class PrismJsAddonSpec : StringSpec() {

    init {
        "prismjs addon loads" {

            TestBed.preact(
                appSetup = {
                    addons {
                        prismJs()
                    }
                },
                view = {
                    PrismJsStatus()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "PrismJsInternals.prism is set after core load" {

            TestBed.preact(
                appSetup = {
                    addons {
                        prismJs()
                    }
                },
                view = {
                    PrismInternalsCheck()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "prism-loaded"
                }
            }
        }
    }
}
