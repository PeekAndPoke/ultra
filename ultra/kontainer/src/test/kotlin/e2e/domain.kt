package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.module

interface MyServiceInterface

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

class S01 {
    val v: Int = 1
}

class S02 {
    val v: Int = 10
}

class S03 {
    val v: Int = 100
}

class S04 {
    val v: Int = 1000
}

class S05 {
    val v: Int = 10000
}

class S06 {
    val v: Int = 100000
}

class S07 {
    val v: Int = 1000000
}

class S08 {
    val v: Int = 10000000
}

class S09 {
    val v: Int = 100000000
}

class S10 {
    val v: Int = 1000000000
}

val common = module {
    singleton(S01::class)
    singleton(S02::class)
    singleton(S03::class)
    singleton(S04::class)
    singleton(S05::class)
    singleton(S06::class)
    singleton(S07::class)
    singleton(S08::class)
    singleton(S09::class)
    singleton(S10::class)
}
