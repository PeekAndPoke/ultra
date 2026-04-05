package io.peekandpoke.kraft.addons.chartjs

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

// Component that subscribes to the chartjs addon and renders loading/ready status
@Suppress("TestFunctionName")
private fun Tag.ChartJsStatus() = comp { ChartJsStatusComponent(it) }

private class ChartJsStatusComponent(ctx: NoProps) : PureComponent(ctx) {
    private val chartJsAddon by subscribingTo(addons.chartJs)

    override fun VDom.render() {
        div(classes = "addon-status") {
            val addon = chartJsAddon
            if (addon != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Component that subscribes to the chartjs addon and calls registerAll() when ready
@Suppress("TestFunctionName")
private fun Tag.ChartJsRegisterAll() = comp { ChartJsRegisterAllComponent(it) }

private class ChartJsRegisterAllComponent(ctx: NoProps) : PureComponent(ctx) {
    private val chartJsAddon by subscribingTo(addons.chartJs)

    override fun VDom.render() {
        div(classes = "addon-status") {
            val addon = chartJsAddon
            if (addon != null) {
                addon.registerAll()
                +"registered"
            } else {
                +"loading"
            }
        }
    }
}

class ChartJsAddonSpec : StringSpec() {

    init {
        "chartjs addon loads and provides ChartJsAddon" {

            TestBed.preact(
                appSetup = {
                    addons {
                        chartJs()
                    }
                },
                view = {
                    ChartJsStatus()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "ChartJsAddon can register modules" {

            TestBed.preact(
                appSetup = {
                    addons {
                        chartJs()
                    }
                },
                view = {
                    ChartJsRegisterAll()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "registered"
                }
            }
        }
    }
}
