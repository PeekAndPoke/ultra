package io.peekandpoke.kraft.addons.avatars

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

@Suppress("TestFunctionName")
private fun Tag.AvatarsLogicComponent() = comp { AvatarsLogicComponent(it) }

private class AvatarsLogicComponent(ctx: NoProps) : PureComponent(ctx) {
    private val avatars by subscribingTo(addons.avatars)

    override fun VDom.render() {
        div(classes = "result") {
            val a = avatars
            if (a == null) {
                +"loading"
            } else {
                val deterministic = a.get("alice") == a.get("alice")
                val distinct = a.get("alice") != a.get("bob")
                val random = a.getRandom() != a.getRandom()
                +"det=$deterministic distinct=$distinct random=$random"
            }
        }
    }
}

/** Deepens the avatars coverage: determinism, name-sensitivity, and randomness of the facade. */
class AvatarsLogicSpec : StringSpec() {

    init {
        "get() is deterministic, name-sensitive, and getRandom() varies" {
            TestBed.preact(
                appSetup = { addons { avatars() } },
                view = { AvatarsLogicComponent() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "det=true distinct=true random=true"
                }
            }
        }
    }
}
