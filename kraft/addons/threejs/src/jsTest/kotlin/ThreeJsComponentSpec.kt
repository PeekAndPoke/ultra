package io.peekandpoke.kraft.addons.threejs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.ops.FrameClock
import kotlinx.coroutines.delay
import kotlinx.html.Tag

/** Frames by hand, for the component's loop. */
private class ComponentClock : FrameClock {
    private val pending = mutableListOf<(Double) -> Unit>()
    private var now = 0.0

    val requests: Int get() = pending.size

    override fun next(onFrame: (timeMs: Double) -> Unit): () -> Unit {
        pending.add(onFrame)
        return { pending.remove(onFrame) }
    }

    fun frame() {
        now += 16.0
        val run = pending.toList()
        pending.clear()
        run.forEach { it(now) }
    }
}

/** A three.js module of the few classes the component builds; its renderer counts the draws. No WebGL needed. */
private class FakeThree {
    var draws = 0

    val module: dynamic = js("({})")

    init {
        val onDraw = { draws++ }

        module.Scene = js("(function () { return function Scene() {}; })")()
        module.PerspectiveCamera = js("(function () { return function Camera() { this.position = { x: 0, y: 0, z: 0 }; }; })")()
        module.WebGLRenderer = js(
            "(function (onDraw) { function R(p) {} " +
                "R.prototype.setPixelRatio = function () {}; R.prototype.setSize = function () {}; " +
                "R.prototype.setClearColor = function () {}; R.prototype.render = function () { onDraw(); }; " +
                "R.prototype.dispose = function () {}; return R; })"
        )(onDraw)
    }
}

@Suppress("TestFunctionName")
private fun Tag.PausableScene(paused: StreamSource<Stream<Boolean>>, clock: FrameClock) =
    comp(PausableScene.Props(paused, clock)) { PausableScene(it) }

@Suppress("TestFunctionName")
private fun Tag.ClockedScene(clock: StreamSource<FrameClock>) = comp(ClockedScene.Props(clock)) { ClockedScene(it) }

/** Renders a ThreeJs on the current value of [Props.clock]: a new clock is a new prop. */
private class ClockedScene(ctx: Ctx<Props>) : Component<ClockedScene.Props>(ctx) {
    data class Props(val clock: StreamSource<FrameClock>)

    private val clock by subscribingTo(props.clock)

    override fun VDom.render() {
        ThreeJs(onReady = {}, clock = clock, width = 100, height = 100)
    }
}

/** Renders a ThreeJs whose `paused` stream is the current value of [Props.paused]: a new stream is a new prop. */
private class PausableScene(ctx: Ctx<Props>) : Component<PausableScene.Props>(ctx) {
    data class Props(val paused: StreamSource<Stream<Boolean>>, val clock: FrameClock)

    private val paused by subscribingTo(props.paused)

    override fun VDom.render() {
        ThreeJs(onReady = {}, paused = paused, clock = props.clock, width = 100, height = 100)
    }
}

/** `ThreeJs(paused = ...)` mounted (ULTRA-00270): the component follows its paused stream, also a new one. */
class ThreeJsComponentSpec : StringSpec({

    suspend fun withScene(paused: StreamSource<Stream<Boolean>>, test: suspend (FakeThree, ComponentClock) -> Unit) {
        val three = FakeThree()
        val clock = ComponentClock()

        TestBed.preact(
            appSetup = { addons { register(key = threeJsAddonKey, name = "threejs-fake") { ThreeJsAddon(three.module) } } },
            view = { PausableScene(paused, clock) },
        ) {
            // Drawn once after onReady, then the loop asks for its frames
            eventually { three.draws shouldBe 1 }
            test(three, clock)
        }
    }

    "a paused stream that turns true asks for no frame; false again draws on" {
        val pausedNow = StreamSource(false)

        withScene(StreamSource(pausedNow)) { three, clock ->
            clock.requests shouldBe 1
            clock.frame()
            three.draws shouldBe 2

            pausedNow(true)
            clock.requests shouldBe 0
            clock.frame()
            three.draws shouldBe 2

            pausedNow(false)
            clock.requests shouldBe 1
            clock.frame()
            three.draws shouldBe 3
        }
    }

    "a new paused stream given after mount is followed, the old one no longer" {
        val first = StreamSource(false)
        val second = StreamSource(true)
        val paused = StreamSource<Stream<Boolean>>(first)

        withScene(paused) { _, clock ->
            clock.requests shouldBe 1

            paused(second)
            delay(20)
            clock.requests shouldBe 0

            // The old stream has no say any more
            first(false)
            clock.requests shouldBe 0

            second(false)
            clock.requests shouldBe 1
        }
    }

    "a new clock given after mount restarts the loop on it; the old one is asked for no more frames" {
        val three = FakeThree()
        val first = ComponentClock()
        val second = ComponentClock()
        val clock = StreamSource<FrameClock>(first)

        TestBed.preact(
            appSetup = { addons { register(key = threeJsAddonKey, name = "threejs-fake") { ThreeJsAddon(three.module) } } },
            view = { ClockedScene(clock) },
        ) {
            eventually { three.draws shouldBe 1 }
            first.requests shouldBe 1

            clock(second)
            delay(20)

            first.requests shouldBe 0
            second.requests shouldBe 1
            second.frame()
            three.draws shouldBe 2
        }
    }
})
