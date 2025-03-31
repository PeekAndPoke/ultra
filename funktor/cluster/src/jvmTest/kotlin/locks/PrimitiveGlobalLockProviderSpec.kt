package de.peekandpoke.funktor.cluster.locks

@Suppress("EXPERIMENTAL_API_USAGE")
class PrimitiveGlobalLockProviderSpec : GlobalLockProviderSpecBase() {
    override fun createSubject() = PrimitiveGlobalLocksProvider()
}
