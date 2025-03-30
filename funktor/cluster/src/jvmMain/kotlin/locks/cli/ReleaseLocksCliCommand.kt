package de.peekandpoke.ktorfx.cluster.locks.cli

import com.github.ajalt.clikt.core.CliktCommand
import de.peekandpoke.ktorfx.cluster.locks.LocksFacade
import kotlinx.coroutines.runBlocking

class ReleaseLocksCliCommand(
    private val locks: LocksFacade,
) : CliktCommand(name = "ktorfx:cluster:release-locks") {

    override fun run() = runBlocking {
        val released = locks.global.releaseBy { true }

        released.forEach {
            println("Released lock '${it.key}' held by '${it.serverId}' created at '${it.created}'")
        }
    }
}
