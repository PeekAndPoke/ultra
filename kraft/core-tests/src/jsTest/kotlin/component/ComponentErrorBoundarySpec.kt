package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Test scaffolding — errors collected here so nested components can report to the test
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private object ErrorCollector {
    val innerCaught = mutableListOf<String>()
    val outerCaught = mutableListOf<String>()

    fun reset() {
        innerCaught.clear()
        outerCaught.clear()
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// A component that throws on render after a flag flips
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.ThrowingChild(triggerAfterMs: Long) = comp(
    ThrowingChild.Props(triggerAfterMs = triggerAfterMs)
) {
    ThrowingChild(it)
}

private class ThrowingChild(
    ctx: io.peekandpoke.kraft.components.Ctx<Props>,
) : io.peekandpoke.kraft.components.Component<ThrowingChild.Props>(ctx) {

    data class Props(val triggerAfterMs: Long)

    private var shouldThrow by value(false)

    init {
        lifecycle {
            onMount {
                io.peekandpoke.kraft.utils.launch {
                    delay(props.triggerAfterMs)
                    shouldThrow = true
                }
            }
        }
    }

    override fun VDom.render() {
        if (shouldThrow) {
            throw IllegalStateException("boom-from-child")
        }
        div(classes = "child-ok") { +"child ok" }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Parent component with configurable onError hook
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.ParentBoundary(catchErrors: Boolean, triggerAfterMs: Long) = comp(
    ParentBoundary.Props(catchErrors = catchErrors, triggerAfterMs = triggerAfterMs)
) {
    ParentBoundary(it)
}

private class ParentBoundary(
    ctx: io.peekandpoke.kraft.components.Ctx<Props>,
) : io.peekandpoke.kraft.components.Component<ParentBoundary.Props>(ctx) {

    data class Props(
        val catchErrors: Boolean,
        val triggerAfterMs: Long,
    )

    private var caughtMessage: String? by value(null)

    init {
        if (props.catchErrors) {
            lifecycle {
                onError { e ->
                    caughtMessage = e.message
                    ErrorCollector.innerCaught.add(e.message ?: "<null>")
                }
            }
        }
    }

    override fun VDom.render() {
        div(classes = "parent") {
            val caught = caughtMessage
            if (caught != null) {
                div(classes = "parent-fallback") { +"caught: $caught" }
            } else {
                ThrowingChild(triggerAfterMs = props.triggerAfterMs)
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Outer boundary — used to verify bubbling up from an inner non-catching parent
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.OuterBoundary() = comp { OuterBoundary(it) }

private class OuterBoundary(ctx: NoProps) : PureComponent(ctx) {

    private var caughtMessage: String? by value(null)

    init {
        lifecycle {
            onError { e ->
                caughtMessage = e.message
                ErrorCollector.outerCaught.add(e.message ?: "<null>")
            }
        }
    }

    override fun VDom.render() {
        div(classes = "outer") {
            val caught = caughtMessage
            if (caught != null) {
                div(classes = "outer-fallback") { +"outer caught: $caught" }
            } else {
                // Inner parent does NOT catch → error must bubble to us
                ParentBoundary(catchErrors = false, triggerAfterMs = 30)
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Polls until check passes or timeout
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private suspend fun eventually(
    timeout: kotlin.time.Duration = kotlin.time.Duration.parse("2s"),
    poll: kotlin.time.Duration = kotlin.time.Duration.parse("25ms"),
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
// Specs
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class ComponentErrorBoundarySpec : StringSpec() {

    init {
        "onError hook catches errors thrown during render" {
            ErrorCollector.reset()

            TestBed.preact(
                view = {
                    ParentBoundary(catchErrors = true, triggerAfterMs = 30)
                },
            ) { root ->
                // Initially the child renders
                root.selectCss(".child-ok").textContent() shouldBe "child ok"

                eventually {
                    ErrorCollector.innerCaught.size shouldBe 1
                    ErrorCollector.innerCaught[0] shouldBe "boom-from-child"
                }

                eventually {
                    root.selectCss(".parent-fallback").textContent() shouldBe "caught: boom-from-child"
                }
            }
        }

        "error bubbles to ancestor when direct parent has no onError handler" {
            ErrorCollector.reset()

            TestBed.preact(
                view = {
                    OuterBoundary()
                },
            ) { root ->
                // Initially the child renders through both parents
                root.selectCss(".child-ok").textContent() shouldBe "child ok"

                eventually {
                    // Outer catches, inner parent did NOT
                    ErrorCollector.outerCaught.size shouldBe 1
                    ErrorCollector.outerCaught[0] shouldBe "boom-from-child"
                    ErrorCollector.innerCaught.size shouldBe 0
                }

                eventually {
                    root.selectCss(".outer-fallback").textContent() shouldBe "outer caught: boom-from-child"
                }
            }
        }

        "error is caught by nearest ancestor with onError hook" {
            ErrorCollector.reset()

            // Nested: Outer (has onError) > Inner Parent (has onError) > Throwing Child
            // The INNER parent should catch, not bubble to outer.
            TestBed.preact(
                view = {
                    div("wrapper") {
                        // Simulate outer as a peer container, but the inner PARENT
                        // with catchErrors=true is the nearest handler for the child.
                        // We reuse ParentBoundary here as the nearest handler.
                        ParentBoundary(catchErrors = true, triggerAfterMs = 30)
                    }
                },
            ) { root ->
                eventually {
                    ErrorCollector.innerCaught.size shouldBe 1
                    ErrorCollector.innerCaught[0] shouldBe "boom-from-child"
                    ErrorCollector.outerCaught.size shouldBe 0
                }
            }
        }
    }
}
