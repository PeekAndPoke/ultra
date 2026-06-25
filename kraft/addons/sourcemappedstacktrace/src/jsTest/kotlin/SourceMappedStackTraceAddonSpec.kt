package io.peekandpoke.kraft.addons.sourcemappedstacktrace

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
    timeout: Duration = 5.seconds,
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

// A synthetic stack with no resolvable source maps; the library should still invoke `done`
// (with the original/best-effort lines) — that callback wiring is what we assert.
private const val SYNTHETIC_STACK =
    "Error: boom\n    at foo (app.js:1:1)\n    at bar (app.js:2:2)"

@Suppress("TestFunctionName")
private fun Tag.SmstLoaderComponent() = comp { SmstLoaderComponent(it) }

private class SmstLoaderComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.sourceMappedStackTrace)

    override fun VDom.render() {
        div(classes = "addon-status") {
            +(if (addon != null) "ready" else "loading")
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.SmstMapComponent() = comp { SmstMapComponent(it) }

private class SmstMapComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.sourceMappedStackTrace)
    private var triggered = false
    private var status by value("pending")

    override fun VDom.render() {
        val a = addon
        if (a != null && !triggered) {
            triggered = true
            a.mapStackTrace(
                stack = SYNTHETIC_STACK,
                done = { result -> status = "frames=${result.size}" },
                options = js("({})"),
            )
        }
        div(classes = "result") {
            +(if (a == null) "loading" else status)
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.SmstCachedComponent() = comp { SmstCachedComponent(it) }

private class SmstCachedComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.sourceMappedStackTrace)
    private var triggered = false
    private var status by value("pending")

    override fun VDom.render() {
        val a = addon
        if (a != null && !triggered) {
            triggered = true
            a.mapStackTraceCached(
                stack = SYNTHETIC_STACK,
                done = { result -> status = "frames=${result.size}" },
            )
        }
        div(classes = "result") {
            +(if (a == null) "loading" else status)
        }
    }
}

class SourceMappedStackTraceAddonSpec : StringSpec() {

    init {
        "sourcemappedstacktrace addon loads and provides the facade" {
            TestBed.preact(
                appSetup = { addons { sourceMappedStackTrace() } },
                view = { SmstLoaderComponent() },
            ) { root ->
                eventually { root.selectCss(".addon-status").textContent() shouldBe "ready" }
            }
        }

        "mapStackTrace invokes the done callback with mapped frames" {
            TestBed.preact(
                appSetup = { addons { sourceMappedStackTrace() } },
                view = { SmstMapComponent() },
            ) { root ->
                eventually {
                    val text = root.selectCss(".result").textContent()
                    (text.startsWith("frames=") && text != "frames=0") shouldBe true
                }
            }
        }

        "mapStackTraceCached invokes the done callback with mapped frames" {
            TestBed.preact(
                appSetup = { addons { sourceMappedStackTrace() } },
                view = { SmstCachedComponent() },
            ) { root ->
                eventually {
                    val text = root.selectCss(".result").textContent()
                    (text.startsWith("frames=") && text != "frames=0") shouldBe true
                }
            }
        }
    }
}
