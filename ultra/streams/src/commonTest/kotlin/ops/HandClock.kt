package io.peekandpoke.ultra.streams.ops

/** A clock a test drives frame by frame. It keeps every request, so a second chain of frames shows as two. */
internal class HandClock : FrameClock {
    private val pending = mutableListOf<(Double) -> Unit>()

    var now = 0.0

    val waiting: Boolean get() = pending.isNotEmpty()

    /** How many frames are asked for at this moment. */
    val requests: Int get() = pending.size

    override fun next(onFrame: (timeMs: Double) -> Unit): () -> Unit {
        pending.add(onFrame)

        return { pending.remove(onFrame) }
    }

    /** One frame, [ms] after the last: every request made before it runs. */
    fun frame(ms: Double = 1000.0 / 60.0) {
        now += ms

        val run = pending.toList()
        pending.clear()
        run.forEach { it(now) }
    }
}
