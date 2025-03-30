package de.peekandpoke.ktorfx.core.fixtures

import de.peekandpoke.ultra.logging.Log
import kotlin.time.measureTimedValue

/**
 * Simple implementation of the [FixtureInstaller] interface.
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
class SimpleFixtureInstaller(
    private val allLoaders: List<FixtureLoader>,
    private val log: Log,
) : FixtureInstaller {

    override fun getLoaders(): List<FixtureLoader> {
        val prioritized = FixtureInstaller.Prioritizer().prioritize(allLoaders).asReversed()

        return prioritized
    }

    override suspend fun clear() {
        // First we need to sort all loaders, so that loaders with no dependencies are executed first
        val prioritized = FixtureInstaller.Prioritizer().prioritize(allLoaders).asReversed()

        // We create a result collector for each loader
        val results: Map<FixtureLoader, FixtureLoader.MutableResult> =
            prioritized.associateWith { FixtureLoader.MutableResult(it) }

        // We run the prepare method for each loader
        prioritized.forEach {
            it.prepare(results[it]!!)
        }

        results.forEach {
            log.info("Cleared fixtures of '${it.value.loader::class.qualifiedName}'")
        }
    }

    override suspend fun install(): FixtureInstaller.Result {
        val result = doInstall(allLoaders)

        log.info("Installing all fixtures took ${result.duration}")

        return result
    }

    override suspend fun installSelected(vararg loader: FixtureLoader): FixtureInstaller.Result {
        // gather all dependent loaders
        val all = FixtureInstaller.DependentsGatherer().gather(loader.toList())
        val result = doInstall(all)

        log.info("Installing ${all.size} fixtures took ${result.duration}")

        return result
    }

    private suspend fun doInstall(loaders: List<FixtureLoader>): FixtureInstaller.Result {

        val results = measureTimedValue {

            // First we need to sort all loaders, so that loaders with no dependencies are executed first
            val prioritized = FixtureInstaller.Prioritizer().prioritize(loaders)

            log.info("Fixture loader order:")

            prioritized.forEachIndexed { idx, loader ->
                log.info("${idx + 1}. ${loader::class.qualifiedName}")
            }

            // We create a result collector for each loader
            val results: Map<FixtureLoader, FixtureLoader.MutableResult> =
                prioritized.associateWith { FixtureLoader.MutableResult(it) }

            // We run the load method for each installer
            prioritized.forEach {
                it.load(results[it]!!)
            }

            // We run the finalize method for each installer
            prioritized.forEach {
                it.finalize(results[it]!!)
            }

            results
        }

        // We return our result
        return FixtureInstaller.Result(
            installer = this,
            loaders = results.value.values.toList(),
            duration = results.duration,
        )
    }
}
