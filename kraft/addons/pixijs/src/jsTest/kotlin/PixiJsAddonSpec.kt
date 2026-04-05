package io.peekandpoke.kraft.addons.pixijs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.pixijs.js.Graphics
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.await
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

// Test component: subscribes to pixiJs addon, renders "loading" then "ready"
@Suppress("TestFunctionName")
private fun Tag.PixiJsLoadConsumer() = comp { PixiJsLoadConsumer(it) }

private class PixiJsLoadConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val pixiAddon by subscribingTo(addons.pixiJs)

    override fun VDom.render() {
        div(classes = "addon-status") {
            if (pixiAddon != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Test component: verifies the addon creates Graphics instances with the expected API
@Suppress("TestFunctionName")
private fun Tag.PixiJsGraphicsConsumer() = comp { PixiJsGraphicsConsumer(it) }

private class PixiJsGraphicsConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val pixiAddon by subscribingTo(addons.pixiJs)
    private var result: String by value("waiting")

    init {
        lifecycle {
            onMount { check() }
            onUpdate { check() }
        }
    }

    private fun check() {
        if (result != "waiting") return
        val addon = pixiAddon ?: return

        try {
            val g: Graphics = addon.createGraphics()
            // Exercise the externals: position, rect + fill, child container
            g.x = 10.0
            g.y = 20.0
            g.rect(0.0, 0.0, 50.0, 30.0).fill(0xff00ff)
            g.circle(100.0, 100.0, 5.0).fill(0x00ff00)
            g.alpha = 0.5
            g.visible = true

            val ok = (g.x == 10.0) && (g.y == 20.0) && (g.alpha == 0.5) && g.visible
            result = if (ok) "ok" else "prop-mismatch"
        } catch (e: Throwable) {
            result = "error: ${e.message}"
        }
        triggerRedraw()
    }

    override fun VDom.render() {
        div(classes = "graphics-status") { +result }
    }
}

// Test component: verifies Application creation and init() promise
@Suppress("TestFunctionName")
private fun Tag.PixiJsApplicationConsumer() = comp { PixiJsApplicationConsumer(it) }

private class PixiJsApplicationConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val pixiAddon by subscribingTo(addons.pixiJs)
    private var result: String by value("waiting")

    init {
        lifecycle {
            onMount { check() }
            onUpdate { check() }
        }
    }

    private fun check() {
        if (result != "waiting") return
        val addon = pixiAddon ?: return

        launch {
            try {
                val app = addon.createApplication()
                app.init(jsObject {
                    this.width = 100
                    this.height = 100
                }).await()

                val screenWidth = app.screen.width

                app.destroy(rendererDestroy = true)

                result = when {
                    screenWidth <= 0 -> "bad-screen-width"
                    else -> "ok"
                }
                triggerRedraw()
            } catch (e: Throwable) {
                result = "error: ${e.message}"
                triggerRedraw()
            }
        }
    }

    override fun VDom.render() {
        div(classes = "app-status") { +result }
    }
}

class PixiJsAddonSpec : StringSpec() {

    init {
        "pixijs addon loads and provides PixiJsAddon" {
            TestBed.preact(
                appSetup = {
                    addons {
                        pixiJs()
                    }
                },
                view = {
                    PixiJsLoadConsumer()
                },
            ) { root ->
                eventually(timeout = 5.seconds) {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "PixiJsAddon creates Graphics with working properties" {
            TestBed.preact(
                appSetup = {
                    addons {
                        pixiJs()
                    }
                },
                view = {
                    PixiJsGraphicsConsumer()
                },
            ) { root ->
                eventually(timeout = 5.seconds) {
                    root.selectCss(".graphics-status").textContent() shouldBe "ok"
                }
            }
        }

        "PixiJsAddon creates and initializes an Application" {
            TestBed.preact(
                appSetup = {
                    addons {
                        pixiJs()
                    }
                },
                view = {
                    PixiJsApplicationConsumer()
                },
            ) { root ->
                eventually(timeout = 10.seconds) {
                    root.selectCss(".app-status").textContent() shouldBe "ok"
                }
            }
        }
    }
}
