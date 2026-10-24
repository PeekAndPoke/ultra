package io.peekandpoke.kraft.coretests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.html.Tag
import kotlinx.html.div

private val lifecycleLog = mutableListOf<String>()

@Suppress("TestFunctionName")
private fun Tag.LifecycleProbe() = comp { LifecycleProbe(it) }

private class LifecycleProbe(ctx: NoProps) : PureComponent(ctx) {
    init {
        lifecycle {
            onMount { lifecycleLog += "mount" }
            onUnmount { lifecycleLog += "unmount" }
        }
    }

    override fun VDom.render() {
        div(classes = "probe") { +"probe" }
    }
}

class TestBedSpec : StringSpec({

    "TestBed unmounts the app after the test, so every component runs its unmount hooks" {
        lifecycleLog.clear()

        TestBed.preact({ LifecycleProbe() }) { root ->
            root.selectCss(".probe").size shouldBe 1
            lifecycleLog shouldBe listOf("mount")
        }

        lifecycleLog shouldBe listOf("mount", "unmount")
        document.getElementById("kraft-testbed") shouldBe null
    }
})
