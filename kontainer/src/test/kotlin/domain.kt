package de.peekandpoke.ultra.kontainer

interface MyServiceInterface

abstract class ConfigIntInjectingBase

data class ConfigIntInjecting(val configInt: Int) : ConfigIntInjectingBase()

data class ConfigAllInjecting(
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
    val string: String,
    val boolean: Boolean
)

open class CounterService {

    private var counter: Int = 0

    fun get() = counter

    fun set(new: Int) {
        counter = new
    }

    fun inc() = counter++
}

class CounterServiceEx01 : CounterService()
class CounterServiceEx02 : CounterService()

class AnotherSimpleService

class SomeIndependentService

data class InjectingService(val simple: CounterService, val another: AnotherSimpleService)

data class AnotherInjectingService(val other: AnotherSimpleService)

data class DeeperInjectingService(val injecting: InjectingService)

interface Ambiguous

class AmbiguousImplOne : Ambiguous

class AmbiguousImplTwo : Ambiguous

data class InjectingAmbiguous(val ambiguous: Ambiguous)

data class InjectingAllAmbiguous(val all: List<Ambiguous>)

data class InjectingSomethingWeird(val weird: Map<String, String>)

data class LazilyInjecting(val lazy: Lazy<CounterService>)
