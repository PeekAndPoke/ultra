package de.peekandpoke.ktorfx.cluster.locks

@Suppress("EXPERIMENTAL_API_USAGE")
class PrimitiveGlobalLockProviderSpec : GlobalLockProviderSpecBase() {
    override fun createSubject() = PrimitiveGlobalLocksProvider()
}
