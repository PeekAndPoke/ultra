package de.peekandpoke.ktorfx.core.fixtures

import de.peekandpoke.ultra.vault.BatchInsertRepository
import de.peekandpoke.ultra.vault.LazyRef
import de.peekandpoke.ultra.vault.Ref
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Storable
import de.peekandpoke.ultra.vault.Stored
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class RepoFixtureLoader<T : Any>(protected val repo: Repository<T>) : FixtureLoader {

    abstract class Fix<F : Any>(val repo: Repository<F>) : () -> Stored<F>, ReadOnlyProperty<Any?, Any?> {

        /**
         * Get the fixture value
         *
         * The fixture must have already been installed. Otherwise, an error is thrown.
         */
        fun get(): Stored<F> {
            return ensure()
        }

        /**
         * Property delegate
         */
        override operator fun getValue(thisRef: Any?, property: KProperty<Any?>) = get()

        /**
         * Gets the fixture value
         *
         * The fixture must have already been installed. Otherwise, an error is thrown.
         */
        override operator fun invoke(): Stored<F> = get()

        /**
         * Reloads the fixture value from the database.
         *
         * This is handy, e.g. when a test is modifying the value and wants to assert the
         * correctness of changes that where applied.
         */
        fun reload(): Stored<F> = runBlocking { repo.findById(get()._id) }
            ?: error("Fixture item '${get()._id}' could not be reloaded from '${repo.name}'")

        /** Get the fixture value as a [Ref] */
        fun asRef(): Ref<F> = get().asRef

        /** Get the fixture value as a [LazyRef] */
        fun asLazyRef(): LazyRef<F> = get().asLazyRef

        /** Installs the fixture into the [repo] */
        internal abstract fun ensure(): Stored<F>

        /** Returns true if the fixtures is already installed */
        internal abstract fun isInstalled(): Boolean

        internal fun isNotInstalled(): Boolean = !isInstalled()
    }

    /**
     * Implementation of [Fix] that is applicable for batch inserts
     */
    internal class BatchableFix<F : Any>(block: suspend () -> Pair<String, F>, repo: Repository<F>) : Fix<F>(repo) {

        internal val block by lazy(LazyThreadSafetyMode.NONE) {
            runBlocking {
                block()
            }
        }

        internal var stored: Stored<F>? = null

        override fun ensure(): Stored<F> = synchronized(this) {
            // Is it already installed
            stored?.let {
                return@synchronized it
            }

            val (id, item) = block

            // Install and return
            runBlocking {
                repo.insert(id, item).apply { stored = this }
            }
        }

        override fun isInstalled(): Boolean = synchronized(this) {
            stored != null
        }
    }

    /**
     * Implementation of [Fix] that is NOT applicable for batch inserts
     */
    internal class SingleFix<F : Any>(private var block: suspend () -> Stored<F>, repo: Repository<F>) : Fix<F>(repo) {

        internal var stored: Stored<F>? = null

        override fun ensure(): Stored<F> = synchronized(this) {
            stored?.let {
                return@synchronized it
            }

            runBlocking { block() }.apply { stored = this }
        }

        override fun isInstalled(): Boolean = synchronized(this) {
            stored != null
        }
    }

    inner class FixtureContext {
        fun <T : Any> key(fix: Fix<T>, infix: String = "") = key(fix(), infix)

        fun <T : Any> key(storable: Storable<T>, infix: String = ""): String {
            val key = listOf(storable._key, infix).filter { it.isNotBlank() }.joinToString("-")

            return key(key)
        }

        fun key(key: String): String {
            val newCount = (keyGroups.getOrDefault(key, 0) + 1).also {
                keyGroups[key] = it
            }

            return if (newCount <= 1) {
                key
            } else {
                "$key-$newCount"
            }
        }
    }

    private val fixtures = mutableListOf<Fix<T>>()

    /**
     * Counts the number of entries for a specific key
     */
    private val keyGroups = mutableMapOf<String, Int>()

    /**
     * Add a fixture that can be batch inserted.
     *
     * Use batch fixtures as much as possible to speed up fixtures installation.
     */
    fun <X : T> fix(block: suspend FixtureContext.() -> Pair<String, X>): Fix<X> {
        @Suppress("UNCHECKED_CAST")
        return BatchableFix(
            block = FixtureContext().let { { it.block() } },
            repo = repo as Repository<X>
        ).apply { fixtures.add(this as Fix<T>) }
    }

    /**
     * Add a fixture that can NOT be batch-inserted.
     *
     * Notice: Only us this one if [fix] is not an option.
     *         Inserting single fixtures is much slower than inserting them as batches.
     */
    fun <X : T> singleFix(block: suspend FixtureContext.() -> Stored<X>): Fix<X> {
        @Suppress("UNCHECKED_CAST")
        return SingleFix(
            block = FixtureContext().let { { it.block() } },
            repo = repo as Repository<X>
        ).apply { fixtures.add(this as Fix<T>) }
    }

    fun get(id: String): Stored<T> = runBlocking { repo.findById(id)!! }

    fun all(): List<Fix<T>> = fixtures.toList()

    override suspend fun prepare(result: FixtureLoader.MutableResult) {
        val removed = repo.removeAll()
        result.info("Cleared repo ${repo.name}. Removed ${removed.count} entries.")
    }

    override suspend fun load(result: FixtureLoader.MutableResult) {

        result.info("Installing fixtures into ${repo.name}")

        // Does the repo support batch inserts
        if (repo is BatchInsertRepository<*>) {

            // Get all batchable fixtures that are not yet installed
            val batchable = fixtures.filterIsInstance<BatchableFix<T>>().filter { it.isNotInstalled() }
            // Batch insert all that have not yet been installed
            @Suppress("UNCHECKED_CAST")
            val results = (repo as BatchInsertRepository<T>).batchInsertPairs(
                batchable.map { it.block }
            )
            // Populate the Fix instances
            results.forEachIndexed { idx, res -> batchable[idx].stored = res }

            // Now install all the rest
            fixtures.minus(batchable.toSet()).forEach {
                it.ensure()
            }

        } else {
            // If no batch insert is possible we insert fixtures one by one
            fixtures.forEach {
                it.ensure()
            }
        }

        fixtures.forEach {
            result.info("+ Created entry with id '${it.get()._id}'")
        }
    }
}
