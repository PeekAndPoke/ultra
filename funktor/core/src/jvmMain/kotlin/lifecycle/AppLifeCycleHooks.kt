package io.peekandpoke.funktor.core.lifecycle

import io.ktor.server.application.*

/** Container for application lifecycle hook implementations, grouped by lifecycle phase. */
class AppLifeCycleHooks(
    val onAppStarting: List<OnAppStarting>,
    val onAppStarted: List<OnAppStarted>,
    val onAppStopPreparing: List<OnAppStopPreparing>,
    val onAppStopping: List<OnAppStopping>,
    val onAppStopped: List<OnAppStopped>,
) {
    /** Determines when a hook runs relative to other hooks; higher priority runs first. */
    class ExecutionOrder private constructor(
        val priority: Int,
    ) : Comparable<ExecutionOrder> {
        companion object {
            @Suppress("unused")
            val ExtremelyEarly = ExecutionOrder(40000)

            @Suppress("unused")
            val VeryEarly = ExecutionOrder(20000)

            @Suppress("unused")
            val Early = ExecutionOrder(10000)

            @Suppress("unused")
            val Normal = ExecutionOrder(0)

            @Suppress("unused")
            val Late = ExecutionOrder(-10000)

            @Suppress("unused")
            val VeryLate = ExecutionOrder(-20000)

            @Suppress("unused")
            val ExtremelyLate = ExecutionOrder(-40000)
        }

        operator fun plus(delta: Int) = ExecutionOrder(priority = priority + delta)

        operator fun minus(delta: Int) = ExecutionOrder(priority = priority - delta)

        override fun compareTo(other: ExecutionOrder): Int {
            return priority.compareTo(other.priority)
        }
    }

    /** Hook invoked synchronously before the application starts (blocking). */
    interface OnAppStarting {
        val executionOrder: ExecutionOrder get() = ExecutionOrder.Normal

        suspend fun onAppStarting(application: Application)
    }

    /** Hook invoked after the application has started. */
    interface OnAppStarted {
        val executionOrder: ExecutionOrder get() = ExecutionOrder.Normal

        suspend fun onAppStarted(application: Application)
    }

    /** Hook invoked when the application is preparing to stop. */
    interface OnAppStopPreparing {
        val executionOrder: ExecutionOrder get() = ExecutionOrder.Normal

        suspend fun onAppStopPreparing(env: ApplicationEnvironment)
    }

    /** Hook invoked when the application is stopping. */
    interface OnAppStopping {
        val executionOrder: ExecutionOrder get() = ExecutionOrder.Normal

        suspend fun onAppStopping(application: Application)
    }

    /** Hook invoked after the application has stopped. */
    interface OnAppStopped {
        val executionOrder: ExecutionOrder get() = ExecutionOrder.Normal

        suspend fun onAppStopped(application: Application)
    }
}
