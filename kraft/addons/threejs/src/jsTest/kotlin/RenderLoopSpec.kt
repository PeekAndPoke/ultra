package io.peekandpoke.kraft.addons.threejs

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.streams.ops.FrameClock

/** A clock the spec drives frame by frame. */
private class HandClock : FrameClock {
    private val pending = mutableListOf<(Double) -> Unit>()

    var now = 0.0

    val requests: Int get() = pending.size

    override fun next(onFrame: (timeMs: Double) -> Unit): () -> Unit {
        pending.add(onFrame)

        return { pending.remove(onFrame) }
    }

    fun frame(ms: Double = 16.0) {
        now += ms

        val run = pending.toList()
        pending.clear()
        run.forEach { it(now) }
    }

    /** Time passes with no frame asked for, as while paused. */
    fun idle(ms: Double) {
        now += ms
    }
}

/** ThreeJsComponent's loop (ULTRA-00270, from Klang): frames, pause and the time across it, without WebGL. */
class RenderLoopSpec : StringSpec({

    "frames run with the delta since the last and the running time" {
        val clock = HandClock()
        val frames = mutableListOf<Pair<Double, Double>>()
        val loop = RenderLoop(clock) { delta, elapsed -> frames.add(delta to elapsed) }

        loop.start()
        repeat(3) { clock.frame(16.0) }

        frames shouldBe listOf(0.0 to 0.0, 16.0 to 16.0, 16.0 to 32.0)
        clock.requests shouldBe 1
    }

    "while paused no frame is asked for at all" {
        val clock = HandClock()
        var frames = 0
        val loop = RenderLoop(clock) { _, _ -> frames++ }

        loop.start()
        clock.frame()
        loop.pause()

        clock.requests shouldBe 0
        repeat(5) { clock.frame() }
        frames shouldBe 1
    }

    "after a resume the first delta is 0 and the elapsed time goes on from where it rested" {
        val clock = HandClock()
        val frames = mutableListOf<Pair<Double, Double>>()
        val loop = RenderLoop(clock) { delta, elapsed -> frames.add(delta to elapsed) }

        loop.start()
        clock.frame(16.0)
        clock.frame(16.0)
        loop.pause()
        clock.idle(60_000.0)
        loop.resume()
        clock.frame(16.0)
        clock.frame(16.0)

        frames shouldBe listOf(0.0 to 0.0, 16.0 to 16.0, 0.0 to 16.0, 16.0 to 32.0)
    }

    "a loop paused before it starts asks for no frame until resumed" {
        val clock = HandClock()
        var frames = 0
        val loop = RenderLoop(clock) { _, _ -> frames++ }

        loop.pause()
        loop.start()
        clock.requests shouldBe 0

        loop.resume()
        clock.frame()
        frames shouldBe 1
    }

    "stop cancels the pending frame; a resume after it starts nothing" {
        val clock = HandClock()
        var frames = 0
        val loop = RenderLoop(clock) { _, _ -> frames++ }

        loop.start()
        loop.pause()
        loop.stop()
        loop.resume()

        clock.requests shouldBe 0
        clock.frame()
        frames shouldBe 0
    }

    "stop alone cancels the pending frame" {
        val clock = HandClock()
        var frames = 0
        val loop = RenderLoop(clock) { _, _ -> frames++ }

        loop.start()
        clock.requests shouldBe 1
        loop.stop()

        clock.requests shouldBe 0
        clock.frame()
        frames shouldBe 0
    }

    "a pause and a resume from inside a frame keep one chain of frames" {
        val clock = HandClock()
        lateinit var loop: RenderLoop
        var frames = 0
        loop = RenderLoop(clock) { _, _ ->
            frames++
            if (frames == 1) {
                loop.pause()
                loop.resume()
            }
        }

        loop.start()
        clock.frame()

        clock.requests shouldBe 1
    }

    "a frame that throws does not end the loop" {
        val clock = HandClock()
        var frames = 0
        val loop = RenderLoop(clock) { _, _ ->
            frames++
            if (frames == 1) error("a bad frame")
        }

        loop.start()
        runCatching { clock.frame() }
        clock.frame()

        frames shouldBe 2
    }

    "a pause from inside a frame asks for no next frame" {
        val clock = HandClock()
        lateinit var loop: RenderLoop
        var frames = 0
        loop = RenderLoop(clock) { _, _ ->
            frames++
            loop.pause()
        }

        loop.start()
        clock.frame()
        clock.frame()

        frames shouldBe 1
        clock.requests shouldBe 0
    }
})
