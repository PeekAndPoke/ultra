package de.peekandpoke.ktorfx.testing

import de.peekandpoke.ktorfx.core.App
import de.peekandpoke.ktorfx.core.KontainerKey
import de.peekandpoke.ktorfx.core.config.AppConfig
import de.peekandpoke.ktorfx.core.fixtures.FixtureInstaller
import de.peekandpoke.ktorfx.core.fixtures.FixtureLoader
import de.peekandpoke.ktorfx.rest.ApiRoute
import de.peekandpoke.ktorfx.testing.TestBed.Companion.createTestBed
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import de.peekandpoke.ultra.vault.Database
import io.kotest.assertions.fail
import io.kotest.common.runBlocking
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeProjectListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.scopes.FreeSpecContainerScope
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.milliseconds

interface AppSpecAware<C : AppConfig> : KontainerAware {
    val spec: AppSpec<C>

    fun testApp(host: String): AppUnderTest<C> = AppUnderTest<C>(testBed = spec.testBed, host = host)
}

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class AppSpec<C : AppConfig>(val app: App.Definition<C>) : FreeSpec(), AppSpecAware<C> {

    private class ServiceProvider<T : Any>(
        val kontainer: () -> Kontainer,
        val cls: KClass<T>,
    ) : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return kontainer().get(cls)
        }
    }

    private class ServiceProviderNullable<T : Any>(
        val kontainer: () -> Kontainer,
        val cls: KClass<T>,
    ) : ReadOnlyProperty<Any?, T?> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return kontainer().getOrNull(cls)
        }
    }

    private class KontainerProperty<T : Any>(
        private val kontainer: () -> Kontainer,
        private val map: (Kontainer) -> T,
    ) : ReadOnlyProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return map(kontainer())
        }
    }

    init {
        extension(object : BeforeProjectListener {
            override suspend fun beforeProject() {
                database?.let {
                    it.ensureRepositories()
                    it.recreateIndexes()
                }
            }
        })

        extension(object : BeforeSpecListener {
            override suspend fun beforeSpec(spec: Spec) {
                clearFixtures()
            }
        })

        extension(object : AfterSpecListener {
            override suspend fun afterSpec(spec: Spec) {
                stopApplication()
            }
        })
    }

    /**
     * Self reference to this AppSpec
     */
    override val spec: AppSpec<C> get() = this

    /**
     * Lazily initializes the `TestBed` instance for the current test environment.
     */
    val testBed: Lazy<TestBed<C>> = lazy { createTestBed() }

    /**
     * Provides access to the `Kontainer` instance associated with the current application environment.
     *
     * This property retrieves the `Kontainer` from the `attributes` of the application's `engine`.
     * It is used to access configured dependencies and services within the context of the test application environment.
     *
     * The [KontainerKey] ensures that the correct `Kontainer` instance is retrieved
     * and allows for dependency injection and service resolution during testing.
     */
    override val kontainer: Kontainer get() = testBed.value.kontainer

    /**
     * Provides access to the test engine.
     *
     * First time the engine is accesses it will be started
     */
    val engine: TestApplication by lazy {
        testBed.value.engine.also {
            runBlocking {
                val rt = Runtime.getRuntime()
                val mem = listOf(
                    rt.freeMemory(),
                    rt.totalMemory(),
                    rt.maxMemory()
                ).joinToString(" / ") { "${it / (1024 * 1024)}MB" }
                println("[${this::class.simpleName}] Starting TestApplicationEngine... $mem")

                it.start()
            }
        }
    }

    /**
     * Provides an optional reference to the [FixtureInstaller] service.
     */
    private val fixtureInstaller by serviceOrNull(FixtureInstaller::class)

    /**
     * Provides an optional reference to a [Database] service.
     */
    private val database by serviceOrNull(Database::class)

    /**
     * Creates and starts a new instance of `TestBed`.
     */
    open fun createTestBed(): TestBed<C> {
        return app.createTestBed()
    }

    /**
     * Provides a delegated property that retrieves a service instance of the specified type.
     */
    inline fun <reified S : Any> service(): ReadOnlyProperty<Any?, S> {
        return service(S::class)
    }

    /**
     * Provides a delegated property for accessing a service of the specified type from the `Kontainer`.
     */
    fun <S : Any> service(cls: KClass<S>): ReadOnlyProperty<Any?, S> {
        return ServiceProvider(kontainer = { kontainer }, cls = cls)
    }

    /**
     * Provides a delegated property that resolves to an optional service of the specified type from the `Kontainer`.
     */
    fun <S : Any> serviceOrNull(cls: KClass<S>): ReadOnlyProperty<Any?, S?> {
        return ServiceProviderNullable(kontainer = { kontainer }, cls = cls)
    }

    /**
     * Provides a delegated property for a specific value derived from the `Kontainer` instance.
     */
    fun <S : Any> kontainer(map: (Kontainer) -> S): ReadOnlyProperty<Any?, S> {
        return KontainerProperty(kontainer = { kontainer }, map = map)
    }

    /**
     * Provides a delegated property that resolves to the `Kontainer` instance.
     */
    fun kontainer(): ReadOnlyProperty<Any?, Kontainer> {
        return kontainer { it }
    }

    /**
     * Clears all installed fixtures.
     *
     * This method uses the `fixtureInstaller` to clear any installed fixtures,
     * such as test data or preconfigured states, that were previously set up.
     *
     * It is a suspend function and should be called when cleaning up resources
     * or resetting the application state is required, typically after test execution.
     *
     * If `fixtureInstaller` is not available, the method will silently do nothing.
     */
    suspend fun clearFixtures() {
        fixtureInstaller?.clear()
    }

    /**
     * Installs all fixtures before the execution of a test Spec.
     *
     * This method integrates a `BeforeSpecListener` extension that ensures all fixtures required
     * for the Spec are installed before the testing starts. The installation process is carried
     * out by invoking the `install` method of the `fixtureInstaller` if it is available.
     * If any errors occur during the installation process, they may result in a failed test.
     */
    fun installAllFixturesBeforeSpec() {
        extension(object : BeforeSpecListener {
            override suspend fun beforeSpec(spec: Spec) {
                fixtureInstaller?.install()
            }
        })
    }

    /**
     * Specifies fixture to be installed before the Spec runs.
     *
     * This method sets up fixture data required for the tests within the Spec.
     * The passed fixture loaders will be invoked along with their dependencies
     * before the execution of the Spec. If any errors occur during fixture installation,
     * the test is marked as failed and the error details are logged.
     *
     * @param loader An array of FixtureLoader instances that specify which fixtures
     * to install before the Spec execution. Each loader may have dependencies
     * and cleanup routines defined in its implementation.
     */
    fun installFixturesBeforeSpec(vararg loader: FixtureLoader) {
        extension(object : BeforeSpecListener {
            override suspend fun beforeSpec(spec: Spec) {
                try {
                    fixtureInstaller?.installSelected(*loader)
                } catch (e: Throwable) {

                    println(e.message)
                    println(e.stackTraceToString())

                    fail(
                        "Error installing Fixtures !!!\n\n" +
                                e.message + "\n" +
                                e.stackTraceToString()
                    )
                }
            }
        })
    }

    /**
     * Stops the test application when it was started.
     */
    suspend fun stopApplication() {
        if (testBed.isInitialized()) {
            val rt = Runtime.getRuntime().also { it.gc() }
            val mem = listOf(
                rt.freeMemory(),
                rt.totalMemory(),
                rt.maxMemory()
            ).joinToString(" / ") { "${it / (1024 * 1024)}MB" }
            println("[${this::class.simpleName}] Stopping TestApplicationEngine... $mem")

            engine.stop()

            delay(50.milliseconds)
        }
    }

    /**
     * Invokes the given test function using the `TestApplicationEngine` instance.
     *
     * @param test A lambda function that will be executed in the context of the `TestApplicationEngine`.
     */
    operator fun TestApplicationEngine.invoke(test: TestApplicationEngine.() -> Any?) {
        this.test()
    }

    /**
     * Invokes the provided test function on the given API route.
     *
     * @param test A suspend function that receives the current ApiRoute instance and executes the test logic.
     */
    operator fun <R, T : ApiRoute<R>> T.invoke(test: suspend FreeSpecContainerScope.(route: T) -> Unit) {
        "${method.value} ${pattern.pattern}" - { test(this@invoke) }
    }
}
