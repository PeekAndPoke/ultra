package de.peekandpoke.funktor.core.fixtures

/**
 * This installer is doing nothing.
 *
 * It must be used when the system is running 'production' mode
 */
class NullFixtureInstaller : FixtureInstaller {

    override fun getLoaders(): List<FixtureLoader> {
        return emptyList()
    }

    override suspend fun clear() {
        // noop
    }

    override suspend fun install(): FixtureInstaller.Result {
        return FixtureInstaller.Result(
            installer = this,
            loaders = emptyList()
        )
    }

    override suspend fun installSelected(vararg loader: FixtureLoader): FixtureInstaller.Result {
        return FixtureInstaller.Result(
            installer = this,
            loaders = emptyList()
        )
    }
}
