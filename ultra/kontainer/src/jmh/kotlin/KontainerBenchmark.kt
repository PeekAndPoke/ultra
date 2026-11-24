package io.peekandpoke.ultra.kontainer.jmh

import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

/**
 * Kontainer creation and service provision on [funktorShapedBlueprint] (ULTRA-00351).
 *
 * Run: `console/wt-gradle.sh :ultra:kontainer:jmh`; iterations and profiler are set in the build file.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class KontainerBenchmark {

    private lateinit var blueprint: KontainerBlueprint
    private lateinit var kontainer: Kontainer
    private lateinit var registry: HandlerRegistry

    private val user = UserProvider("user-1")
    private val requestInfo = RequestInfo("/api/v1/things")

    @Setup(Level.Trial)
    fun setup() {
        blueprint = funktorShapedBlueprint()
        // Validates the blueprint and creates every global singleton once, as a running app has
        request(blueprint.create(), null)

        kontainer = blueprint.create { with(UserProvider::class) { user } }
        registry = kontainer.get(HandlerRegistry::class)
        // Warm the hot gets
        kontainer.get(S100::class)
        kontainer.get(D45::class)
    }

    // Creation /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Benchmark
    fun create(): Kontainer = blueprint.create()

    @Benchmark
    fun createOneOverride(): Kontainer = blueprint.create { with(UserProvider::class) { user } }

    @Benchmark
    fun createTwoOverrides(): Kontainer = blueprint.create {
        with(UserProvider::class) { user }
        with(RequestInfo::class) { requestInfo }
    }

    @Benchmark
    fun cloneOneOverride(): Kontainer = kontainer.clone { with(UserProvider::class) { user } }

    /** A recorded request: create with the user, then the services an API call touches */
    @Benchmark
    fun request(bh: Blackhole) {
        request(blueprint.create { with(UserProvider::class) { user } }, bh)
    }

    // Provision ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Benchmark
    fun getStaticHot(): Any = kontainer.get(S100::class)

    @Benchmark
    fun getDynamicHot(): Any = kontainer.get(D45::class)

    @Benchmark
    fun getOrNullInterface(): Any? = kontainer.getOrNull(Clock::class)

    @Benchmark
    fun getAll5(): List<Handler> = kontainer.getAll(Handler::class)

    @Benchmark
    fun prototypeNoArg(): Any = kontainer.get(P0::class)

    @Benchmark
    fun prototype3Args(): Any = kontainer.get(P1::class)

    @Benchmark
    fun prototypeFactory(): Any = kontainer.get(P2::class)

    @Benchmark
    fun lazyLookupGet(): Any = registry.handlers.get(Handler3::class)

    // 8 threads ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Benchmark
    @Threads(8)
    fun createT8(): Kontainer = blueprint.create()

    @Benchmark
    @Threads(8)
    fun requestT8(bh: Blackhole) {
        request(blueprint.create { with(UserProvider::class) { user } }, bh)
    }

    @Benchmark
    @Threads(8)
    fun getStaticHotT8(): Any = kontainer.get(S100::class)

    /** The services one API call touches; [bh] is null only in the setup, where the results are dropped */
    private fun request(k: Kontainer, bh: Blackhole?) {
        val touched = arrayOf(
            k.get(UserProvider::class),
            k.get(SD39::class),
            k.get(SD25::class),
            k.get(S130::class),
            k.get(D45::class),
            k.get(P1::class),
            k.getAll(Handler::class),
            k.getOrNull(Clock::class),
            k.get(HandlerRegistry::class),
            k.get(LazyUser::class).s.value,
        )

        bh?.consume(touched)
    }
}
