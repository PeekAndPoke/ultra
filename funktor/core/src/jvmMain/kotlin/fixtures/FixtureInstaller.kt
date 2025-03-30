package de.peekandpoke.ktorfx.core.fixtures

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Common interface for all fixture installers.
 */
interface FixtureInstaller {
    /**
     * Sorts loaders so that dependent loaders are last in the list.
     */
    class Prioritizer {

        fun prioritize(loaders: List<FixtureLoader>): List<FixtureLoader> {

            val result = mutableListOf<FixtureLoader>()

            loaders.forEach {
                result.addAllNew(
                    getDepsFlat(it)
                )
            }

            return result
        }

        private fun getDepsFlat(loader: FixtureLoader): List<FixtureLoader> {

            val result = mutableListOf<FixtureLoader>()

            getDepsFlatRecursive(loader, result, emptySet())

            return result.toList()
        }

        private fun getDepsFlatRecursive(
            loader: FixtureLoader,
            results: MutableList<FixtureLoader>,
            path: Set<FixtureLoader>,
        ) {

            if (path.contains(loader)) {
                val inPath = path.map { it::class.qualifiedName }.joinToString(", ")
                error("Cyclic dependency of FixtureLoaders detected: ${loader::class} -> $inPath")
            }

            loader.dependsOn.forEach {
                getDepsFlatRecursive(it, results, path.plus(loader))
            }

            results.addWhenNew(loader)
        }

        private fun MutableList<FixtureLoader>.addWhenNew(loader: FixtureLoader) {
            if (loader !in this) {
                add(loader)
            }
        }

        private fun MutableList<FixtureLoader>.addAllNew(loaders: List<FixtureLoader>) {
            loaders.forEach { addWhenNew(it) }
        }
    }

    /**
     * Gathers all loaders that the given loaders depend on.
     */
    class DependentsGatherer {

        fun gather(loaders: List<FixtureLoader>): List<FixtureLoader> {

            var previousSize: Int
            var result = loaders.toSet()

            do {
                previousSize = result.size
                result = result.flatMap { loader ->
                    listOf(loader)
                        .plus(loader.dependsOn)
                        .plus(loader.completesWith.map { it.value })
                }.toSet()
            } while (previousSize < result.size)

            return result.toList()
        }
    }

    /**
     * The Result produced by installing the fixtures
     */
    data class Result(
        val installer: FixtureInstaller,
        val loaders: List<FixtureLoader.Result>,
        val duration: Duration = 0.seconds,
    ) {
        val successful get() = loaders.all { it.isOk }
    }

    /**
     * Get all registered loaders
     */
    fun getLoaders(): List<FixtureLoader>

    /**
     * Clears and installs all fixtures
     */
    suspend fun clearAndInstall(): Result {
        clear()
        return install()
    }

    /**
     * Clears all fixtures
     */
    suspend fun clear()

    /**
     * Installs all fixtures
     */
    suspend fun install(): Result

    /**
     * Installed only selected [loader]s and the loaders that these depend on.
     */
    suspend fun installSelected(vararg loader: FixtureLoader): Result
}
