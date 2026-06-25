package io.peekandpoke.kraft.addons.nxcompile

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

// NOTE: @nx-js/compiler-util's compileCode is a *statement executor*, not an expression evaluator.
// The compiled function takes a sandbox object and runs `with (sandbox) { <code> }`, discarding any
// return value. So code mutates the sandbox and we read the result back from it.

@Suppress("TestFunctionName")
private fun Tag.NxCompileLoaderComponent() = comp { NxCompileLoaderComponent(it) }

private class NxCompileLoaderComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.nxCompile)

    override fun VDom.render() {
        div(classes = "addon-status") {
            +(if (addon != null) "ready" else "loading")
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.NxCompileSandboxComponent() = comp { NxCompileSandboxComponent(it) }

private class NxCompileSandboxComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.nxCompile)

    override fun VDom.render() {
        div(classes = "result") {
            val a = addon
            if (a == null) {
                +"loading"
            } else {
                val sb: dynamic = js("({ a: 6, b: 7, out: 0 })")
                a.compileCode("out = a * b")(sb)
                +"${sb.out}"
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.NxCompileControlFlowComponent() = comp { NxCompileControlFlowComponent(it) }

private class NxCompileControlFlowComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.nxCompile)

    override fun VDom.render() {
        div(classes = "result") {
            val a = addon
            if (a == null) {
                +"loading"
            } else {
                // multi-statement code, not just an expression
                val sb: dynamic = js("({ n: 5, out: \"\" })")
                a.compileCode("if (n > 3) { out = 'big' } else { out = 'small' }")(sb)
                +"${sb.out}"
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.NxCompileIsolationComponent() = comp { NxCompileIsolationComponent(it) }

private class NxCompileIsolationComponent(ctx: NoProps) : PureComponent(ctx) {
    private val addon by subscribingTo(addons.nxCompile)

    override fun VDom.render() {
        div(classes = "result") {
            val a = addon
            if (a == null) {
                +"loading"
            } else {
                // The sandbox hides host globals — Math is not visible inside compiled code.
                val sb: dynamic = js("({ out: \"\" })")
                a.compileCode("out = typeof Math")(sb)
                +"${sb.out}"
            }
        }
    }
}

class NxCompileAddonSpec : StringSpec() {

    init {
        "nxcompile addon loads and provides NxCompileAddon" {
            TestBed.preact(
                appSetup = { addons { nxCompile() } },
                view = { NxCompileLoaderComponent() },
            ) { root ->
                eventually { root.selectCss(".addon-status").textContent() shouldBe "ready" }
            }
        }

        "compileCode runs code against a sandbox and writes back results" {
            TestBed.preact(
                appSetup = { addons { nxCompile() } },
                view = { NxCompileSandboxComponent() },
            ) { root ->
                eventually { root.selectCss(".result").textContent() shouldBe "42" }
            }
        }

        "compileCode runs multi-statement control flow" {
            TestBed.preact(
                appSetup = { addons { nxCompile() } },
                view = { NxCompileControlFlowComponent() },
            ) { root ->
                eventually { root.selectCss(".result").textContent() shouldBe "big" }
            }
        }

        "compileCode sandboxes host globals away from compiled code" {
            TestBed.preact(
                appSetup = { addons { nxCompile() } },
                view = { NxCompileIsolationComponent() },
            ) { root ->
                eventually { root.selectCss(".result").textContent() shouldBe "undefined" }
            }
        }
    }
}
