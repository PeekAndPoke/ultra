package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.kraft.utils.onResize
import io.peekandpoke.kraft.utils.onWindowBlur
import io.peekandpoke.kraft.utils.onWindowFocus
import io.peekandpoke.kraft.utils.onWindowResize
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.events.Event
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

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// onResize — uses ResizeObserver on the component's DOM element
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.ResizableBox() = comp { ResizableBox(it) }

private class ResizableBox(ctx: NoProps) : PureComponent(ctx) {

    private var resizeCount: Int by value(0)

    init {
        lifecycle {
            onResize { _ ->
                resizeCount += 1
            }
        }
    }

    override fun VDom.render() {
        div(classes = "box") {
            +"count=$resizeCount"
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Component exposing window lifecycle hooks to external counters
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.WindowHooksConsumer(
    onResize: (Vector2D) -> Unit,
    onFocus: () -> Unit,
    onBlur: () -> Unit,
) = comp(
    WindowHooksConsumer.Props(onResize, onFocus, onBlur)
) { WindowHooksConsumer(it) }

private class WindowHooksConsumer(
    ctx: Ctx<Props>,
) : Component<WindowHooksConsumer.Props>(ctx) {

    data class Props(
        val onResize: (Vector2D) -> Unit,
        val onFocus: () -> Unit,
        val onBlur: () -> Unit,
    )

    init {
        lifecycle {
            onWindowResize { size -> props.onResize(size) }
            onWindowFocus { props.onFocus() }
            onWindowBlur { props.onBlur() }
        }
    }

    override fun VDom.render() {
        div(classes = "window-hooks") { +"ok" }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Specs
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class ResizeHooksSpec : StringSpec() {

    init {
        "onResize fires when the component's DOM element is observed" {
            TestBed.preact(
                view = { ResizableBox() },
            ) { root ->
                // ResizeObserver fires once on initial observation with the current size.
                eventually {
                    val text = root.selectCss(".box").textContent()
                    (text.contains("count=") && !text.contains("count=0")) shouldBe true
                }
            }
        }

        "onWindowResize fires with Vector2D when window.resize is dispatched" {
            var resizeCount = 0
            var lastSize: Vector2D? = null

            TestBed.preact(
                view = {
                    WindowHooksConsumer(
                        onResize = { size -> resizeCount += 1; lastSize = size },
                        onFocus = {},
                        onBlur = {},
                    )
                },
            ) { root ->
                root.selectCss(".window-hooks").textContent() shouldBe "ok"

                window.dispatchEvent(Event("resize"))

                eventually {
                    resizeCount shouldBe 1
                }

                val size = lastSize
                size shouldBe Vector2D(window.innerWidth.toDouble(), window.innerHeight.toDouble())
            }
        }

        "onWindowFocus and onWindowBlur fire on window focus/blur events" {
            var focusCount = 0
            var blurCount = 0

            TestBed.preact(
                view = {
                    WindowHooksConsumer(
                        onResize = {},
                        onFocus = { focusCount += 1 },
                        onBlur = { blurCount += 1 },
                    )
                },
            ) { root ->
                root.selectCss(".window-hooks").textContent() shouldBe "ok"

                window.dispatchEvent(Event("focus"))
                window.dispatchEvent(Event("focus"))
                window.dispatchEvent(Event("blur"))

                eventually {
                    focusCount shouldBe 2
                    blurCount shouldBe 1
                }
            }
        }

        "window listeners auto-unsubscribe on unmount" {
            var resizeCount = 0
            val toggle = ToggleSource()

            TestBed.preact(
                view = {
                    ToggleContainer(
                        toggle = toggle,
                        child = {
                            WindowHooksConsumer(
                                onResize = { resizeCount += 1 },
                                onFocus = {},
                                onBlur = {},
                            )
                        },
                    )
                },
            ) { root ->
                // Child is mounted; first resize should fire the callback
                root.selectCss(".window-hooks").textContent() shouldBe "ok"
                window.dispatchEvent(Event("resize"))
                eventually { resizeCount shouldBe 1 }

                // Unmount the child by flipping the toggle
                toggle.set(false)

                eventually {
                    root.selectCss(".window-hooks-placeholder").textContent() shouldBe "hidden"
                }

                // More resize events should NOT increment the counter
                val before = resizeCount
                window.dispatchEvent(Event("resize"))
                window.dispatchEvent(Event("resize"))
                delay(50)
                resizeCount shouldBe before
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// ToggleContainer — renders `child` when toggle stream emits true, otherwise a placeholder
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private class ToggleSource {
    val stream = StreamSource(true)
    fun set(v: Boolean) {
        stream(v)
    }
}

@Suppress("TestFunctionName")
private fun Tag.ToggleContainer(
    toggle: ToggleSource,
    child: FlowContent.() -> Unit,
) = comp(
    ToggleContainer.Props(toggle = toggle, child = child)
) { ToggleContainer(it) }

private class ToggleContainer(
    ctx: Ctx<Props>,
) : Component<ToggleContainer.Props>(ctx) {

    data class Props(
        val toggle: ToggleSource,
        val child: FlowContent.() -> Unit,
    )

    private val show by subscribingTo(props.toggle.stream)

    override fun VDom.render() {
        div(classes = "toggle-container") {
            if (show) {
                props.child(this)
            } else {
                div(classes = "window-hooks-placeholder") { +"hidden" }
            }
        }
    }
}
